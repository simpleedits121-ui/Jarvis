package com.example.jarvis

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun CharacterEnginePanel() {
    var mood by remember { mutableStateOf("Idle") }
    var tapCount by remember { mutableIntStateOf(0) }

    val transition = rememberInfiniteTransition(label = "jarvis_idle")

    val breathing by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.012f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    val floating by transition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "JARVIS • $mood",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ) {
            Image(
                painter = painterResource(id = R.drawable.character_sheet),
                contentDescription = "JARVIS anime companion. Tap to interact.",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .graphicsLayer {
                        scaleX = breathing
                        scaleY = breathing
                        translationY = floating
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                tapCount++
                                mood = when (tapCount % 4) {
                                    1 -> "Happy"
                                    2 -> "Playful"
                                    3 -> "Curious"
                                    else -> "Idle"
                                }
                            },
                            onLongPress = {
                                mood = "Listening"
                            }
                        )
                    },
                contentScale = ContentScale.Fit
            )
        }

        Text(
            text = when (mood) {
                "Listening" -> "I'm listening. What's on your mind?"
                "Thinking" -> "Give me a moment to think."
                "Speaking" -> "I'm ready to respond."
                "Sleeping" -> "Rest mode activated."
                "Happy" -> "Hey! Nice to see you!"
                "Playful" -> "You tapped me again!"
                "Curious" -> "Hmm? What are we doing?"
                else -> "Your anime companion"
            },
            style = MaterialTheme.typography.bodyLarge
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MoodButton("Idle", Modifier.weight(1f)) { mood = "Idle" }
            MoodButton("Listen", Modifier.weight(1f)) { mood = "Listening" }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MoodButton("Think", Modifier.weight(1f)) { mood = "Thinking" }
            MoodButton("Speak", Modifier.weight(1f)) { mood = "Speaking" }
            MoodButton("Sleep", Modifier.weight(1f)) { mood = "Sleeping" }
        }
    }
}

@Composable
private fun MoodButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(label)
    }
}
