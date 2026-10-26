
package com.example.jarvis

class CharacterController(
    private val onStateChanged: (CharacterState) -> Unit
) {
    var state: CharacterState = CharacterState()
        private set

    private fun update(next: CharacterState) {
        state = next
        onStateChanged(next)
    }

    fun onUserMessage(text: String) {
        update(
            state.copy(
                mood = CharacterMood.LISTENING,
                text = text,
                isSpeaking = false,
                mouthOpen = 0f
            )
        )
    }

    fun onAiThinking() {
        update(
            state.copy(
                mood = CharacterMood.THINKING,
                isSpeaking = false,
                mouthOpen = 0f
            )
        )
    }

    fun onAiResponse(reply: CharacterReply) {
        val mood = when (reply.emotion?.lowercase()) {
            "happy", "excited" -> CharacterMood.HAPPY
            "playful", "teasing" -> CharacterMood.PLAYFUL
            "sad" -> CharacterMood.SAD
            "concerned", "empathetic" -> CharacterMood.CONCERNED
            "surprised" -> CharacterMood.SURPRISED
            "thinking" -> CharacterMood.THINKING
            "sleeping" -> CharacterMood.SLEEPING
            else -> CharacterMood.IDLE
        }

        val gesture = when (reply.gesture?.lowercase()) {
            "head_tilt" -> CharacterGesture.HEAD_TILT
            "nod" -> CharacterGesture.NOD
            "shake_head" -> CharacterGesture.SHAKE_HEAD
            "wave" -> CharacterGesture.WAVE
            else -> CharacterGesture.NONE
        }

        update(
            state.copy(
                mood = mood,
                gesture = gesture,
                text = reply.text,
                intensity = reply.intensity.coerceIn(0f, 1f),
                isSpeaking = false,
                mouthOpen = 0f
            )
        )
    }

    fun onListening() {
        update(state.copy(mood = CharacterMood.LISTENING))
    }

    fun onSpeechStarted() {
        update(
            state.copy(
                isSpeaking = true,
                mouthOpen = 0.2f
            )
        )
    }

    fun onSpeechLevel(level: Float) {
        if (state.isSpeaking) {
            update(
                state.copy(
                    mouthOpen = level.coerceIn(0f, 1f)
                )
            )
        }
    }

    fun onSpeechEnded() {
        update(
            state.copy(
                mood = CharacterMood.IDLE,
                gesture = CharacterGesture.NONE,
                isSpeaking = false,
                mouthOpen = 0f
            )
        )
    }

    fun onBecameIdle() {
        update(
            state.copy(
                mood = CharacterMood.IDLE,
                gesture = CharacterGesture.NONE,
                isSpeaking = false,
                mouthOpen = 0f
            )
        )
    }
}
