
package com.example.jarvis

import android.content.Context

/**
 * Process-wide owner of the character's persistent daily routine.
 */
object CharacterRoutineManager {

    @Volatile
    private var scheduler: CharacterRoutineScheduler? = null

    @Synchronized
    fun initialize(context: Context) {
        if (scheduler != null) {
            scheduler?.refresh()
            return
        }

        val instance = CharacterRoutineScheduler(context.applicationContext) {
            activity -> CharacterStateStore.setRoutineActivity(activity)
        }

        scheduler = instance
        instance.start()
    }

    fun noteInteraction() {
        scheduler?.noteInteraction()
    }

    fun refresh() {
        scheduler?.refresh()
    }

    fun settings(): CharacterRoutineSettings {
        return scheduler?.settings() ?: CharacterRoutineSettings()
    }

    fun saveSettings(settings: CharacterRoutineSettings) {
        scheduler?.saveSettings(settings)
    }
}
