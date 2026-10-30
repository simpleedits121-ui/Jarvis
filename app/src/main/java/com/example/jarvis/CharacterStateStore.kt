
package com.example.jarvis

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * One source of truth for the character within the app process.
 * Both the main renderer and floating service observe this state.
 */
object CharacterStateStore {

    private val _state = MutableStateFlow(CharacterState())

    val state: StateFlow<CharacterState> = _state.asStateFlow()

    fun update(transform: (CharacterState) -> CharacterState) {
        _state.update(transform)
    }

    fun set(next: CharacterState) {
        _state.value = next
    }

    fun setRoutineActivity(activity: RoutineActivity) {
        update { current ->
            when (activity) {
                RoutineActivity.SLEEPING -> current.copy(
                    mood = CharacterMood.SLEEPING,
                    gesture = CharacterGesture.NONE,
                    isSpeaking = false,
                    mouthOpen = 0f
                )

                RoutineActivity.READING -> current.copy(
                    mood = CharacterMood.IDLE,
                    gesture = CharacterGesture.NONE,
                    isSpeaking = false,
                    mouthOpen = 0f
                )

                // Keep the active interaction's expression intact.
                RoutineActivity.ENGAGED -> current
            }
        }
    }
}
