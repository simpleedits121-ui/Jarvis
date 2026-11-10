package com.example.jarvis.aether

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Build
import android.os.PowerManager
import java.util.Locale

/**
 * AETHER platform foundation. It only manages resources owned by this process.
 * It does not alter Android settings, access other apps, or request root/overlay access.
 */
object AetherEngine {
    @Volatile private var trimPressure: Int = 0

    data class RenderPlan(
        val renderScale: Float,
        val targetFps: Int,
        val maxResidentMb: Int,
        val maxDrawCalls: Int,
        val maxTriangles: Int
    )

    data class Status(
        val nativeLoaded: Boolean,
        val vulkanDescription: String,
        val plan: RenderPlan,
        val deviceMemoryClassMb: Int,
        val lowRamDevice: Boolean
    )

    fun onTrimMemory(level: Int) {
        trimPressure = when {
            level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> 3
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> 2
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> 1
            else -> 0
        }
    }

    fun createStatus(context: Context, foreground: Boolean = true): Status {
        val app = context.applicationContext
        val activityManager = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val powerManager = app.getSystemService(Context.POWER_SERVICE) as PowerManager
        val memoryClass = activityManager.memoryClass.coerceAtLeast(64)
        val thermal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            powerManager.currentThermalStatus
        } else 0
        val saver = powerManager.isPowerSaveMode
        val loaded = AetherNative.isAvailable()
        val description = if (loaded) {
            runCatching { AetherNative.nativeProbe() }
                .getOrElse { "Vulkan probe failed: ${it.javaClass.simpleName}" }
        } else {
            "Native AETHER library unavailable; check NDK/ABI build."
        }
        val raw = if (loaded) {
            runCatching {
                AetherNative.nativePlan(
                    memoryClass, activityManager.isLowRamDevice, thermal,
                    foreground, saver, trimPressure
                )
            }.getOrNull()
        } else null
        val plan = if (raw != null && raw.size >= 5) {
            RenderPlan(raw[0], raw[1].toInt(), raw[2].toInt(), raw[3].toInt(), raw[4].toInt())
        } else {
            RenderPlan(0.65f, 15, 32, 900, 250_000)
        }
        return Status(loaded, description, plan, memoryClass, activityManager.isLowRamDevice)
    }

    fun format(status: Status): String = String.format(
        Locale.US,
        "AETHER %s • %d FPS • %.0f%% scale • %d MB working-set target",
        if (status.nativeLoaded) "NATIVE" else "FALLBACK",
        status.plan.targetFps,
        status.plan.renderScale * 100f,
        status.plan.maxResidentMb
    )
}

private object AetherNative {
    private val loaded = runCatching { System.loadLibrary("aether"); true }.getOrDefault(false)
    fun isAvailable(): Boolean = loaded

    external fun nativeProbe(): String
    external fun nativePlan(
        memoryClassMb: Int,
        lowMemory: Boolean,
        thermalStatus: Int,
        foreground: Boolean,
        batterySaver: Boolean,
        trimPressure: Int
    ): FloatArray
}
