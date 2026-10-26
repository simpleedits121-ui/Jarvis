
package com.example.jarvis

enum class CharacterMood {
    IDLE,
    LISTENING,
    THINKING,
    HAPPY,
    PLAYFUL,
    SAD,
    CONCERNED,
    SURPRISED,
    SLEEPING
}

enum class CharacterGesture {
    NONE,
    HEAD_TILT,
    NOD,
    SHAKE_HEAD,
    WAVE
}

data class CharacterState(
    val mood: CharacterMood = CharacterMood.IDLE,
    val gesture: CharacterGesture = CharacterGesture.NONE,
    val text: String = "",
    val intensity: Float = 0.4f,
    val isSpeaking: Boolean = false,
    val mouthOpen: Float = 0f
)

data class CharacterReply(
    val text: String,
    val emotion: String? = null,
    val gesture: String? = null,
    val intensity: Float = 0.6f
)
