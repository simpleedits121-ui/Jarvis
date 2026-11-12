
package com.example.jarvis

import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class AetherQualityPreset {
    LOW,
    BALANCED,
    HIGH
}

data class AetherQualitySettings(
    val preset: AetherQualityPreset,
    val targetFps: Int,
    val renderScale: Float,
    val shadowsEnabled: Boolean,
    val physicsUpdatesPerSecond: Int
)

object AetherQualityProfiles {
    fun get(preset: AetherQualityPreset) = when (preset) {
        AetherQualityPreset.LOW -> AetherQualitySettings(
            preset = preset,
            targetFps = 30,
            renderScale = 0.70f,
            shadowsEnabled = false,
            physicsUpdatesPerSecond = 20
        )

        AetherQualityPreset.BALANCED -> AetherQualitySettings(
            preset = preset,
            targetFps = 45,
            renderScale = 0.85f,
            shadowsEnabled = false,
            physicsUpdatesPerSecond = 30
        )

        AetherQualityPreset.HIGH -> AetherQualitySettings(
            preset = preset,
            targetFps = 60,
            renderScale = 1.0f,
            shadowsEnabled = true,
            physicsUpdatesPerSecond = 60
        )
    }
}

/**
 * Adjusts rendering quality using measured frame times.
 * The renderer will consume these settings in a later phase.
 */
class AetherPerformanceGovernor(
    initialPreset: AetherQualityPreset =
        AetherQualityPreset.BALANCED
) {
    var settings: AetherQualitySettings =
        AetherQualityProfiles.get(initialPreset)
        private set

    private var averageFrameMs = 16.7
    private var slowFrames = 0
    private var fastFrames = 0

    @Synchronized
    fun onFrameRendered(frameTimeMs: Double): AetherQualitySettings {
        if (!frameTimeMs.isFinite() || frameTimeMs <= 0.0) {
            return settings
        }

        averageFrameMs =
            averageFrameMs * 0.9 + frameTimeMs * 0.1

        val targetFrameMs = 1000.0 / settings.targetFps

        if (averageFrameMs > targetFrameMs * 1.25) {
            slowFrames++
            fastFrames = 0
        } else if (averageFrameMs < targetFrameMs * 0.78) {
            fastFrames++
            slowFrames = 0
        } else {
            slowFrames = 0
            fastFrames = 0
        }

        if (slowFrames >= 30) {
            changePreset(step = -1)
            slowFrames = 0
        } else if (fastFrames >= 180) {
            changePreset(step = 1)
            fastFrames = 0
        }

        return settings
    }

    @Synchronized
    fun setPreset(preset: AetherQualityPreset) {
        settings = AetherQualityProfiles.get(preset)
        slowFrames = 0
        fastFrames = 0
    }

    private fun changePreset(step: Int) {
        val presets = AetherQualityPreset.values()
        val current = presets.indexOf(settings.preset)
        val next = (current + step).coerceIn(
            presets.indices
        )
        settings = AetherQualityProfiles.get(presets[next])
    }
}

sealed class AetherModelInspection {
    data class Valid(
        val version: Int,
        val declaredLength: Long,
        val actualLength: Long
    ) : AetherModelInspection()

    data class Invalid(
        val reason: String
    ) : AetherModelInspection()
}

/**
 * Offline structural validation for binary glTF (.glb) version 2.
 * A valid result does NOT guarantee that a model is rigged.
 */
object AetherGlbInspector {

    private const val GLB_MAGIC = 0x46546C67
    private const val JSON_CHUNK = 0x4E4F534A

    fun inspect(file: File): AetherModelInspection {
        if (!file.exists() || !file.isFile) {
            return invalid("Model file does not exist.")
        }

        if (!file.extension.equals("glb", ignoreCase = true)) {
            return invalid("Please select a .glb model.")
        }

        val actualLength = file.length()

        if (actualLength < 20) {
            return invalid("File is too small to be a valid GLB.")
        }

        return try {
            val header = ByteArray(20)

            FileInputStream(file).use { input ->
                if (input.read(header) != header.size) {
                    return invalid("Could not read GLB header.")
                }
            }

            val buffer = ByteBuffer.wrap(header)
                .order(ByteOrder.LITTLE_ENDIAN)

            val magic = buffer.int
            val version = buffer.int
            val declaredLength = buffer.int.toLong() and
                0xFFFFFFFFL
            val jsonLength = buffer.int.toLong() and
                0xFFFFFFFFL
            val jsonType = buffer.int

            if (magic != GLB_MAGIC) {
                return invalid("Invalid GLB file signature.")
            }

            if (version != 2) {
                return invalid("Only GLB version 2 is supported.")
            }

            if (declaredLength != actualLength) {
                return invalid("GLB file length does not match its header.")
            }

            if (jsonType != JSON_CHUNK) {
                return invalid("GLB JSON chunk is missing.")
            }

            if (jsonLength == 0L ||
                jsonLength > declaredLength - 20L
            ) {
                return invalid("Invalid GLB JSON chunk length.")
            }

            AetherModelInspection.Valid(
                version = version,
                declaredLength = declaredLength,
                actualLength = actualLength
            )
        } catch (e: Exception) {
            invalid("Unable to inspect model: ${e.message}")
        }
    }

    private fun invalid(reason: String) =
        AetherModelInspection.Invalid(reason)
}
