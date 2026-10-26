
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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun CharacterEnginePanel() {
    var mood by remember { mutableStateOf("Idle") }

    val transition = rememberInfiniteTransition(label = "character")
    val breathing by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("JARVIS • $mood")

        Image(
            painter = painterResource(id = R.drawable.character_sheet),
            contentDescription = "JARVIS anime scientist",
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .scale(breathing),
            contentScale = ContentScale.Fit
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { mood = "Listening" }) {
                Text("🎤 Listen")
            }
            Button(onClick = { mood = "Thinking" }) {
                Text("🧠 Think")
            }
            Button(onClick = { mood = "Speaking" }) {
                Text("💬 Speak")
            }
            Button(onClick = { mood = "Sleeping" }) {
                Text("🌙 Sleep")
            }
        }
    }
}

