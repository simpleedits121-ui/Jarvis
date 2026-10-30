package com.example.jarvis

enum class CharacterDisplayMode {
    AUTO,
    FULL_BODY,
    HALF_BODY
}

enum class CharacterAnimationQuality {
    BATTERY_SAVER,
    BALANCED,
    HIGH_QUALITY
}

data class CharacterDisplayContext(
    val isFloatingCompanion: Boolean = false,
    val availableWidthDp: Float = 0f,
    val isCompactWindow: Boolean = false
)

object CharacterDisplayResolver {

    fun resolve(
        preference: CharacterDisplayMode,
        context: CharacterDisplayContext
    ): CharacterDisplayMode {

        if (preference != CharacterDisplayMode.AUTO) {
            return preference
        }

        return if (
            context.isFloatingCompanion ||
            context.isCompactWindow ||
            (context.availableWidthDp > 0f &&
             context.availableWidthDp < 420f)
        ) {
            CharacterDisplayMode.HALF_BODY
        } else {
            CharacterDisplayMode.FULL_BODY
        }
    }
}
