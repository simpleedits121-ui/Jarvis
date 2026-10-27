
package com.example.jarvis

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.core.JarvisCore
import com.example.jarvis.core.Message

private val Background = Color(0xFF07111D)
private val Panel = Color(0xFF101F30)
private val Cyan = Color(0xFF63D8FF)
private val Muted = Color(0xFF8CA9BF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Cyan,
                    background = Background,
                    surface = Panel
                )
            ) {
                JarvisDesktop(
                    onStartFloating = { startFloatingCompanion() },
                    onStopFloating = { stopService(Intent(this, DesktopMateService::class.java)) }
                )
            }
        }
    }

    private fun startFloatingCompanion() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(this)
        ) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return
        }

        startService(Intent(this, DesktopMateService::class.java))
    }
}

@Composable
private fun JarvisDesktop(
    onStartFloating: () -> Unit,
    onStopFloating: () -> Unit
) {
    var selected by remember { mutableStateOf("Home") }
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .width(58.dp)
                .fillMaxHeight()
                .background(Panel, RoundedCornerShape(16.dp))
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("J", color = Cyan, fontSize = 28.sp,
                fontWeight = FontWeight.Bold)

            HorizontalDivider(color = Color(0xFF263B4D))

            listOf("Home", "Chat", "Models", "Tools", "Settings")
                .forEach { page ->
                    NavigationTile(
                        label = page,
                        selected = selected == page,
                        onClick = { selected = page }
                    )
                }

            Spacer(Modifier.weight(1f))
            Text("v0.3", color = Muted, fontSize = 10.sp)
        }

        Spacer(Modifier.width(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Panel, RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "JARVIS WORKSPACE",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "LOCAL DESKTOP COMPANION",
                        color = Muted,
                        fontSize = 8.sp
                    )
                }

                Box {
                    TextButton(onClick = { menuOpen = true }) {
                        Text("•••", color = Cyan, fontSize = 18.sp)
                    }

                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        listOf("Home", "Chat", "Models", "Tools", "Settings")
                            .forEach { page ->
                                DropdownMenuItem(
                                    text = { Text(page) },
                                    onClick = {
                                        selected = page
                                        menuOpen = false
                                    }
                                )
                            }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            when (selected) {
                "Chat" -> ChatPanel()
                "Models" -> InfoPanel(
                    "MODEL MANAGER",
                    "Offline GGUF model installation and switching are planned."
                )
                "Tools" -> InfoPanel(
                    "DEVICE TOOLS",
                    "Device actions and app controls are not connected yet."
                )
                "Settings" -> InfoPanel(
                    "SETTINGS",
                    "Character, voice and appearance controls will be added here."
                )
                else -> HomePanel(
                    onOpenChat = { selected = "Chat" },
                    onStartFloating = onStartFloating,
                    onStopFloating = onStopFloating
                )
            }
        }
    }
}

@Composable
private fun NavigationTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val symbol = when (label) {
        "Home" -> "⌂"
        "Chat" -> "◉"
        "Models" -> "▤"
        "Tools" -> "⚙"
        else -> "☰"
    }

    Surface(
        onClick = onClick,
        color = if (selected) Color(0xFF16445B) else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.size(50.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(symbol, color = Cyan, fontSize = 20.sp)
            Text(
                label,
                color = if (selected) Color.White else Muted,
                fontSize = 7.sp
            )
        }
    }
}

@Composable
private fun HomePanel(
    onOpenChat: () -> Unit,
    onStartFloating: () -> Unit,
    onStopFloating: () -> Unit
) {
    var mood by remember { mutableStateOf("Idle") }
    var babyMode by remember { mutableStateOf(false) }
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapAt by remember { mutableLongStateOf(0L) }
    var blinkFrame by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(3600L)
            blinkFrame = true
            kotlinx.coroutines.delay(140L)
            blinkFrame = false
        }
    }

    val transition = rememberInfiniteTransition(label = "jarvis_character")
    val breathing by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.014f,
        animationSpec = infiniteRepeatable(
            animation = tween(2100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )
    val floating by transition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gentle_float"
    )
    val targetTilt = when (mood) {
        "Listening" -> -1.8f
        "Thinking" -> 2.0f
        "Happy" -> -1.0f
        "Sleeping" -> 1.2f
        else -> 0f
    }
    val moodTilt by animateFloatAsState(
        targetValue = targetTilt,
        animationSpec = tween(420),
        label = "mood_tilt"
    )
    val moodGlow by animateColorAsState(
        targetValue = when (mood) {
            "Listening" -> Color(0xFF123B55)
            "Thinking" -> Color(0xFF252348)
            "Happy" -> Color(0xFF16443F)
            "Sleeping" -> Color(0xFF171F3B)
            else -> Color(0xFF0B1A2A)
        },
        animationSpec = tween(500),
        label = "mood_glow"
    )

    val moodLine = when (mood) {
        "Listening" -> "I'm listening. Go ahead."
        "Thinking" -> "Give me a moment to think."
        "Happy" -> "Good to see you again."
        "Sleeping" -> "Rest mode. Tap me to wake up."
        else -> "Your local anime companion"
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = moodGlow,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "JARVIS • $mood",
                    color = Cyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(6.dp))

                val characterImage = when {
                    babyMode -> R.drawable.character_baby
                    blinkFrame && mood == "Idle" -> R.drawable.character_wink
                    mood == "Happy" -> R.drawable.character_happy
                    mood == "Listening" -> R.drawable.character_surprised
                    mood == "Thinking" -> R.drawable.character_thinking
                    mood == "Sleeping" -> R.drawable.character_sleeping
                    else -> R.drawable.character_portrait
                }

                Image(
                    painter = painterResource(id = characterImage),
                    contentDescription = if (babyMode) "JARVIS baby companion" else "JARVIS anime scientist companion",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .graphicsLayer {
                            scaleX = breathing
                            scaleY = breathing
                            translationY = floating
                            rotationZ = moodTilt
                        }
                        .pointerInput(babyMode) {
                            detectTapGestures(
                                onTap = {
                                    val now = System.currentTimeMillis()
                                    tapCount = if (now - lastTapAt <= 650L) tapCount + 1 else 1
                                    lastTapAt = now
                                    if (tapCount >= 3) {
                                        babyMode = !babyMode
                                        tapCount = 0
                                        mood = "Happy"
                                    } else {
                                        mood = when (mood) {
                                            "Idle" -> "Happy"
                                            "Happy" -> "Listening"
                                            "Listening" -> "Thinking"
                                            "Thinking" -> "Sleeping"
                                            else -> "Idle"
                                        }
                                    }
                                },
                                onLongPress = { mood = "Listening" }
                            )
                        },
                    contentScale = ContentScale.Fit
                )

                Spacer(Modifier.height(4.dp))
                Text(
                    if (babyMode) "JARVIS • BABY MODE" else "JARVIS SCIENCE COMPANION",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (babyMode) "Baby mode • triple-tap to switch back" else "$moodLine • triple-tap for baby mode",
                    color = Muted,
                    fontSize = 10.sp
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    listOf("Idle", "Listen", "Think", "Happy", "Sleep").forEach { action ->
                        Button(
                            onClick = {
                                mood = when (action) {
                                    "Listen" -> "Listening"
                                    "Think" -> "Thinking"
                                    "Sleep" -> "Sleeping"
                                    else -> action
                                }
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 1.dp, vertical = 7.dp)
                        ) {
                            Text(action, fontSize = 9.sp, maxLines = 1)
                        }
                    }
                }
                Text(
                    "Animated illustration prototype • Live2D rig not connected yet",
                    color = Muted,
                    fontSize = 9.sp
                )
            }
        }

        Surface(
            color = Panel,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    "QUICK ACCESS",
                    color = Muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = onOpenChat, modifier = Modifier.fillMaxWidth()) {
                    Text("Open JARVIS Chat")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onStartFloating,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
                    ) { Text("Start Floating", fontSize = 11.sp) }
                    OutlinedButton(
                        onClick = onStopFloating,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
                    ) { Text("Stop Floating", fontSize = 11.sp) }
                }
                Text(
                    "If asked, allow Display over other apps, return here, then tap Start Floating again.",
                    color = Muted,
                    fontSize = 10.sp
                )
                Text(
                    "Chat currently uses the demo response engine.",
                    color = Muted,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun InfoPanel(title: String, description: String) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Panel,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                title,
                color = Cyan,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(description, color = Color.White, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ChatPanel() {
    val core = remember { JarvisCore() }
    var characterState by remember { mutableStateOf(CharacterState()) }
    val characterController = remember {
        CharacterController { nextState -> characterState = nextState }
    }
    val messages = remember {
        mutableStateListOf(
            Message(
                fromUser = false,
                text = "Hello. I'm JARVIS. My current response engine is a demo."
            )
        )
    }
    var draft by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "CONVERSATION",
            color = Cyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Surface(
            color = Panel,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(10.dp)) {
                Text(
                    "CHARACTER: ${characterState.mood.name}",
                    color = Cyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Gesture: ${characterState.gesture.name}",
                    color = Muted,
                    fontSize = 10.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { message ->
                Surface(
                    color = if (message.fromUser)
                        Color(0xFF164E68) else Panel,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            if (message.fromUser) "YOU" else "JARVIS",
                            color = Cyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            message.text,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Panel, RoundedCornerShape(22.dp))
                .padding(start = 14.dp, end = 5.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f).padding(vertical = 10.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontSize = 14.sp
                ),
                decorationBox = { inner ->
                    Box {
                        if (draft.isEmpty()) {
                            Text(
                                "Talk to JARVIS...",
                                color = Muted,
                                fontSize = 13.sp
                            )
                        }
                        inner()
                    }
                }
            )

            IconButton(onClick = {
                val prompt = draft.trim()
                if (prompt.isNotEmpty()) {
                    characterController.onUserMessage(prompt)
                    messages.add(Message(fromUser = true, text = prompt))

                    characterController.onAiThinking()
                    val replyText = core.respondToDemoMessage(prompt)
                    val lower = prompt.lowercase()
                    val emotion = when {
                        "thank" in lower || "great" in lower -> "happy"
                        "sad" in lower || "upset" in lower -> "empathetic"
                        "help" in lower || "confused" in lower -> "concerned"
                        "?" in prompt -> "thinking"
                        else -> null
                    }
                    val gesture = when {
                        lower == "hello" || lower == "hi" ||
                            lower.startsWith("hello ") || lower.startsWith("hi ") -> "wave"
                        "thank" in lower -> "nod"
                        "?" in prompt -> "head_tilt"
                        else -> null
                    }

                    characterController.onAiResponse(
                        CharacterReply(
                            text = replyText,
                            emotion = emotion,
                            gesture = gesture
                        )
                    )
                    messages.add(Message(fromUser = false, text = replyText))
                    draft = ""
                }
            }) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Cyan
                )
            }
        }
    }
}
