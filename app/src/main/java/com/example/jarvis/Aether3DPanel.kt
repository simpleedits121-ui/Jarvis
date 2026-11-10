package com.example.jarvis

import android.content.Context
import android.os.Build
import android.os.Choreographer
import android.os.PowerManager
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.filament.Engine
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import java.nio.ByteBuffer

@Composable
fun Aether3DPanel() {
    var backend by remember { mutableStateOf("Initializing renderer…") }
    var modelStatus by remember { mutableStateOf("Preparing offline character asset…") }
    var fps by remember { mutableStateOf(30) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07111D))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "AETHER • 3D CHARACTER LAB",
            color = Color(0xFF63D8FF),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            "Offline glTF renderer • Vulkan preferred • touch to orbit / pinch to zoom",
            color = Color(0xFF8CA9BF),
            fontSize = 11.sp
        )

        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = { ctx ->
                AetherModelSurfaceView(ctx).also { view ->
                    backend = view.backendLabel
                    modelStatus = view.modelStatus
                    fps = view.targetFps
                }
            }
        )

        Text("$backend • adaptive cap $fps FPS", color = Color(0xFF63D8FF), fontSize = 12.sp)
        Text(modelStatus, color = Color(0xFF8CA9BF), fontSize = 10.sp)
    }
}

/** Filament ModelViewer surface: Vulkan first, OpenGL ES fallback. */
private class AetherModelSurfaceView(context: Context) : SurfaceView(context) {
    private val choreographer = Choreographer.getInstance()
    private val viewer: ModelViewer
    private var renderLoopActive = false
    private var lastFrameNanos = 0L

    val backendLabel: String
    val targetFps: Int = chooseTargetFps(context)
    var modelStatus: String = "Model not loaded"
        private set

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!renderLoopActive || !isAttachedToWindow) return
            val frameInterval = 1_000_000_000L / targetFps.coerceAtLeast(10)
            if (lastFrameNanos == 0L || frameTimeNanos - lastFrameNanos >= frameInterval) {
                viewer.render(frameTimeNanos)
                lastFrameNanos = frameTimeNanos
            }
            choreographer.postFrameCallback(this)
        }
    }

    init {
        initializeFilament()
        setBackgroundColor(android.graphics.Color.rgb(5, 12, 22))
        val choice = createPreferredEngine()
        backendLabel = choice.second
        viewer = ModelViewer(this, choice.first)
        viewer.autoPlayAnimations = true
        setOnTouchListener(viewer)

        modelStatus = try {
            val bytes = context.assets.open("models/rem.glb").use { it.readBytes() }
            require(bytes.size > 20) { "Converted Rem GLB is empty." }
            viewer.loadModelGlb(ByteBuffer.wrap(bytes))
            viewer.transformToUnitCube()
            val clips = viewer.animator?.animationCount ?: 0
            if (clips > 0) {
                "Rem model loaded • $clips skeletal animation clip(s) • auto-play enabled"
            } else {
                "Rem model loaded • no skeletal animation clips detected in source FBX"
            }
        } catch (error: Exception) {
            "Model asset missing or invalid: ${error.message ?: error.javaClass.simpleName}. Run the AETHER model-conversion workflow."
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startRenderLoop()
    }

    override fun onDetachedFromWindow() {
        stopRenderLoop()
        // ModelViewer's own detach listener releases Filament resources.
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) startRenderLoop() else stopRenderLoop()
    }

    private fun startRenderLoop() {
        if (!renderLoopActive && isAttachedToWindow && windowVisibility == VISIBLE) {
            renderLoopActive = true
            lastFrameNanos = 0L
            choreographer.postFrameCallback(frameCallback)
        }
    }

    private fun stopRenderLoop() {
        renderLoopActive = false
        choreographer.removeFrameCallback(frameCallback)
    }

    private fun createPreferredEngine(): Pair<Engine, String> {
        val vulkan = runCatching { Engine.create(Engine.Backend.VULKAN) }.getOrNull()
        if (vulkan != null) return vulkan to "Filament Vulkan"
        val openGl = Engine.create(Engine.Backend.OPENGL)
            ?: throw IllegalStateException("Neither Vulkan nor OpenGL ES renderer could be initialized.")
        return openGl to "Filament OpenGL ES fallback"
    }

    companion object {
        @Volatile private var filamentInitialized = false

        @Synchronized
        private fun initializeFilament() {
            if (!filamentInitialized) {
                Utils.init()
                filamentInitialized = true
            }
        }

        private fun chooseTargetFps(context: Context): Int {
            val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val thermal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) power.currentThermalStatus else 0
            return when {
                thermal >= PowerManager.THERMAL_STATUS_SEVERE -> 15
                thermal >= PowerManager.THERMAL_STATUS_MODERATE || power.isPowerSaveMode -> 24
                else -> 30
            }
        }
    }
}
