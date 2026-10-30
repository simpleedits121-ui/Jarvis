
package com.example.jarvis

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.Calendar

enum class RoutineActivity {
    SLEEPING,
    READING,
    ENGAGED
}

data class CharacterRoutineSettings(
    val sleepHour: Int = 22,
    val sleepMinute: Int = 0,
    val wakeHour: Int = 5,
    val wakeMinute: Int = 0,
    val resumeAfterMinutes: Int = 2,
    val enabled: Boolean = true
)

class CharacterRoutineScheduler(
    context: Context,
    private val onActivityChanged: (RoutineActivity) -> Unit
) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(
        "jarvis_character_routine",
        Context.MODE_PRIVATE
    )
    private val handler = Handler(Looper.getMainLooper())

    private var running = false
    private var lastPublished: RoutineActivity? = null

    private val scheduledRefresh = Runnable {
        if (running) refreshAndSchedule()
    }

    fun settings(): CharacterRoutineSettings {
        return CharacterRoutineSettings(
            sleepHour = preferences.getInt("sleep_hour", 22),
            sleepMinute = preferences.getInt("sleep_minute", 0),
            wakeHour = preferences.getInt("wake_hour", 5),
            wakeMinute = preferences.getInt("wake_minute", 0),
            resumeAfterMinutes = preferences.getInt("resume_minutes", 2)
                .coerceIn(1, 120),
            enabled = preferences.getBoolean("enabled", true)
        )
    }

    fun saveSettings(settings: CharacterRoutineSettings) {
        require(settings.sleepHour in 0..23)
        require(settings.wakeHour in 0..23)
        require(settings.sleepMinute in 0..59)
        require(settings.wakeMinute in 0..59)
        require(settings.resumeAfterMinutes in 1..120)

        preferences.edit()
            .putInt("sleep_hour", settings.sleepHour)
            .putInt("sleep_minute", settings.sleepMinute)
            .putInt("wake_hour", settings.wakeHour)
            .putInt("wake_minute", settings.wakeMinute)
            .putInt("resume_minutes", settings.resumeAfterMinutes)
            .putBoolean("enabled", settings.enabled)
            .apply()

        refreshAndSchedule()
    }

    /**
     * Call when the user taps the character, sends a message,
     * starts speaking, or otherwise interacts with JARVIS.
     *
     * Every interaction restarts the inactivity countdown.
     */
    fun noteInteraction() {
        preferences.edit()
            .putLong("last_interaction_ms", System.currentTimeMillis())
            .apply()

        refreshAndSchedule()
    }

    /**
     * Recalculate after app/service startup, returning to foreground,
     * or a system time/time-zone change.
     */
    fun refresh() {
        refreshAndSchedule()
    }

    fun start() {
        if (running) {
            refreshAndSchedule()
            return
        }

        running = true
        refreshAndSchedule()
    }

    fun stop() {
        running = false
        handler.removeCallbacks(scheduledRefresh)
    }

    fun currentActivity(nowMs: Long = System.currentTimeMillis()): RoutineActivity {
        val config = settings()

        if (!config.enabled) return RoutineActivity.READING

        val lastInteraction = preferences.getLong("last_interaction_ms", 0L)
        val timeoutMs = config.resumeAfterMinutes * 60_000L

        if (
            lastInteraction > 0L &&
            nowMs >= lastInteraction &&
            nowMs - lastInteraction < timeoutMs
        ) {
            return RoutineActivity.ENGAGED
        }

        return scheduledActivity(nowMs, config)
    }

    private fun refreshAndSchedule() {
        if (!running) {
            publish(currentActivity())
            return
        }

        val activity = currentActivity()
        publish(activity)

        handler.removeCallbacks(scheduledRefresh)

        val config = settings()
        val now = System.currentTimeMillis()
        val lastInteraction = preferences.getLong("last_interaction_ms", 0L)
        val timeoutMs = config.resumeAfterMinutes * 60_000L

        val remainingOverride = if (
            lastInteraction > 0L &&
            now >= lastInteraction &&
            now - lastInteraction < timeoutMs
        ) {
            timeoutMs - (now - lastInteraction)
        } else {
            0L
        }

        val nextDelay = if (remainingOverride > 0L) {
            remainingOverride
        } else {
            millisUntilNextBoundary(now, config)
        }

        handler.postDelayed(
            scheduledRefresh,
            nextDelay.coerceAtLeast(1_000L)
        )
    }

    private fun publish(activity: RoutineActivity) {
        if (activity != lastPublished) {
            lastPublished = activity
            onActivityChanged(activity)
        }
    }

    private fun scheduledActivity(
        nowMs: Long,
        config: CharacterRoutineSettings
    ): RoutineActivity {
        val now = Calendar.getInstance().apply {
            timeInMillis = nowMs
        }

        val currentMinute = now.get(Calendar.HOUR_OF_DAY) * 60 +
            now.get(Calendar.MINUTE)

        val sleepMinute = config.sleepHour * 60 + config.sleepMinute
        val wakeMinute = config.wakeHour * 60 + config.wakeMinute

        // Equal times mean the routine is ambiguous; default to reading.
        if (sleepMinute == wakeMinute) return RoutineActivity.READING

        val sleeping = if (sleepMinute > wakeMinute) {
            currentMinute >= sleepMinute || currentMinute < wakeMinute
        } else {
            currentMinute >= sleepMinute && currentMinute < wakeMinute
        }

        return if (sleeping) {
            RoutineActivity.SLEEPING
        } else {
            RoutineActivity.READING
        }
    }

    private fun millisUntilNextBoundary(
        nowMs: Long,
        config: CharacterRoutineSettings
    ): Long {
        if (!config.enabled) return 60 * 60 * 1000L

        val now = Calendar.getInstance().apply {
            timeInMillis = nowMs
        }

        val currentMinute = now.get(Calendar.HOUR_OF_DAY) * 60 +
            now.get(Calendar.MINUTE)
        val sleepMinute = config.sleepHour * 60 + config.sleepMinute
        val wakeMinute = config.wakeHour * 60 + config.wakeMinute

        if (sleepMinute == wakeMinute) return 60 * 60 * 1000L

        val sleeping = if (sleepMinute > wakeMinute) {
            currentMinute >= sleepMinute || currentMinute < wakeMinute
        } else {
            currentMinute >= sleepMinute && currentMinute < wakeMinute
        }

        val targetMinute = if (sleeping) wakeMinute else sleepMinute

        val target = Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, targetMinute / 60)
            set(Calendar.MINUTE, targetMinute % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= nowMs) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        return target.timeInMillis - nowMs
    }
}
