package com.example.jarvis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.aether.AetherEngine

@Composable
fun AetherStatusCard() {
    val context = LocalContext.current
    var status by remember { mutableStateOf<AetherEngine.Status?>(null) }
    LaunchedEffect(context) {
        status = AetherEngine.createStatus(context)
    }
    Surface(
        color = Color(0xFF0B2634),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text("AETHER CHARACTER ENGINE", color = Color(0xFF63D8FF), fontSize = 11.sp)
            Text(
                status?.let(AetherEngine::format) ?: "Checking local Vulkan capability…",
                color = Color.White,
                fontSize = 12.sp
            )
            Text(
                status?.vulkanDescription ?: "No network access required",
                color = Color(0xFF8CA9BF),
                fontSize = 9.sp
            )
            Text(
                "0.1 foundation: Vulkan capability probe + adaptive resource planner. GLB rendering is the next milestone.",
                color = Color(0xFF8CA9BF),
                fontSize = 9.sp
            )
        }
    }
}
