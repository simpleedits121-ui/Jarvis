
package com.example.jarvis

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun CharacterAvatar(
    state: CharacterState,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(
        label = "character"
    )

    val breathing by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    val moodText = when (state.mood) {
        CharacterMood.IDLE -> "Idle"
        CharacterMood.LISTENING -> "Listening..."
        CharacterMood.THINKING -> "Thinking..."
        CharacterMood.HAPPY -> "Happy"
        CharacterMood.PLAYFUL -> "Playful"
        CharacterMood.SAD -> "Sad"
        CharacterMood.CONCERNED -> "Concerned"
        CharacterMood.SURPRISED -> "Surprised!"
        CharacterMood.SLEEPING -> "Sleeping..."
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "JARVIS • $moodText",
            style = MaterialTheme.typography.titleLarge
        )

        Image(
            painter = painterResource(
                id = R.drawable.character_sheet
            ),
            contentDescription = "JARVIS character",
            modifier = Modifier
                .size(width = 240.dp, height = 300.dp)
                .scale(breathing),
            contentScale = ContentScale.Fit
        )

        if (state.text.isNotBlank()) {
            Text(
                text = state.text,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun CharacterInteractionDemo(
    modifier: Modifier = Modifier
) {
    var state by remember {
        mutableStateOf(CharacterState())
    }

    val controller = remember {
        CharacterController { newState ->
            state = newState
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CharacterAvatar(state = state)

        Button(onClick = { controller.onListening() }) {
            Text("Listen")
        }

        Button(onClick = { controller.onAiThinking() }) {
            Text("Think")
        }

        Button(
            onClick = {
                controller.onAiResponse(
                    CharacterReply(
                        text = "Oh, that's actually interesting!",
                        emotion = "playful",
                        gesture = "head_tilt"
                    )
                )
            }
        ) {
            Text("Playful")
        }

        Button(
            onClick = {
                controller.onAiResponse(
                    CharacterReply(
                        text = "I'm here. What's wrong?",
                        emotion = "concerned"
                    )
                )
            }
        ) {
            Text("Concerned")
        }

        Button(onClick = { controller.onBecameIdle() }) {
            Text("Return to idle")
        }
    }
}
