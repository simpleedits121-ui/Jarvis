
package com.example.jarvis

class CharacterController(
    private val onStateChanged: (CharacterState) -> Unit = {}
) {
    var state: CharacterState = CharacterStateStore.state.value
        private set

    private fun update(transform: (CharacterState) -> CharacterState) {
        CharacterStateStore.update(transform)
        state = CharacterStateStore.state.value
        onStateChanged(state)
    }

    fun onUserMessage(text: String) {
        CharacterRoutineManager.noteInteraction()
        update {
            it.copy(
                mood = CharacterMood.LISTENING,
                text = text,
                gesture = CharacterGesture.NONE,
                isSpeaking = false,
                mouthOpen = 0f
            )
        }
    }

    fun onAiThinking() {
        update {
            it.copy(
                mood = CharacterMood.THINKING,
                isSpeaking = false,
                mouthOpen = 0f
            )
        }
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

        update {
            it.copy(
                mood = mood,
                gesture = gesture,
                text = reply.text,
                intensity = reply.intensity.coerceIn(0f, 1f),
                isSpeaking = false,
                mouthOpen = 0f
            )
        }
    }

    fun onListening() {
        CharacterRoutineManager.noteInteraction()
        update {
            it.copy(
                mood = CharacterMood.LISTENING,
                isSpeaking = false,
                mouthOpen = 0f
            )
        }
    }

    fun onSpeechStarted() {
        update {
            it.copy(
                isSpeaking = true,
                mouthOpen = 0.2f
            )
        }
    }

    fun onSpeechLevel(level: Float) {
        update {
            if (it.isSpeaking) {
                it.copy(mouthOpen = level.coerceIn(0f, 1f))
            } else {
                it
            }
        }
    }

    fun onSpeechEnded() {
        update {
            it.copy(
                mood = CharacterMood.IDLE,
                gesture = CharacterGesture.NONE,
                isSpeaking = false,
                mouthOpen = 0f
            )
        }
    }

    fun onBecameIdle() {
        update {
            it.copy(
                mood = CharacterMood.IDLE,
                gesture = CharacterGesture.NONE,
                isSpeaking = false,
                mouthOpen = 0f
            )
        }
    }

    fun wakeUp() {
        CharacterRoutineManager.noteInteraction()
        update {
            it.copy(
                mood = CharacterMood.HAPPY,
                gesture = CharacterGesture.NOD,
                isSpeaking = false,
                mouthOpen = 0f
            )
        }
    }
}
