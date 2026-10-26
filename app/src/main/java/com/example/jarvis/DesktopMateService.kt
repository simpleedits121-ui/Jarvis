
package com.example.jarvis

import android.app.Service
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.speech.tts.TextToSpeech
import java.util.Locale
import kotlin.math.sin

class DesktopMateService : Service(), TextToSpeech.OnInitListener {

    private lateinit var windowManager: WindowManager
    private lateinit var mateView: MateView
    private lateinit var params: WindowManager.LayoutParams
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var attached = false

    private val handler = Handler(Looper.getMainLooper())
    private var lastMove = 0L
    private var direction = 1
    private var lastTap = 0L
    private var tapCount = 0
    private var lastX = 0
    private var lastY = 0
    private var dragging = false

    private val wander = object : Runnable {
        override fun run() {
            if (!attached) return

            val now = SystemClock.uptimeMillis()
            if (!dragging && now - lastMove > 3500L) {
                val bounds = resources.displayMetrics
                val maxX = (bounds.widthPixels - params.width).coerceAtLeast(0)
                val maxY = (bounds.heightPixels - params.height).coerceAtLeast(0)

                params.x += direction * 2

                if (params.x < 0 || params.x > maxX) {
                    direction *= -1
                    params.x = params.x.coerceIn(0, maxX)
                    mateView.expression = MateView.Expression.HAPPY
                }

                params.y = params.y.coerceIn(0, maxY)

                try {
                    windowManager.updateViewLayout(mateView, params)
                } catch (_: Exception) {
                    stopSelf()
                    return
                }
            }

            handler.postDelayed(this, 80L)
        }
    }

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(this)
        ) {
            stopSelf()
            return
        }

        windowManager =
            getSystemService(WINDOW_SERVICE) as WindowManager

        tts = TextToSpeech(this, this)
        mateView = MateView(this)

        val windowType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

        params = WindowManager.LayoutParams(
            dp(116),
            dp(190),
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12)
            y = resources.displayMetrics.heightPixels / 3
        }

        mateView.onGesture = { event, dx, dy ->
            when (event) {
                MateView.Gesture.DOWN -> {
                    lastX = params.x
                    lastY = params.y
                    lastMove = SystemClock.uptimeMillis()
                    dragging = false
                    true
                }

                MateView.Gesture.MOVE -> {
                    if (kotlin.math.abs(dx) > dp(4) ||
                        kotlin.math.abs(dy) > dp(4)
                    ) {
                        dragging = true
                    }

                    if (dragging) {
                        params.x = (lastX + dx).coerceAtLeast(0)
                        params.y = (lastY + dy).coerceAtLeast(0)
                        try {
                            windowManager.updateViewLayout(mateView, params)
                        } catch (_: Exception) {
                            stopSelf()
                        }
                    }
                    true
                }

                MateView.Gesture.UP -> {
                    if (!dragging) handleTap()
                    lastMove = SystemClock.uptimeMillis()
                    true
                }
            }
        }

        try {
            windowManager.addView(mateView, params)
            attached = true
            handler.postDelayed(wander, 80L)
        } catch (_: Exception) {
            stopSelf()
        }
    }

    private fun handleTap() {
        val now = SystemClock.uptimeMillis()

        if (now - lastTap < 350L) {
            tapCount++
        } else {
            tapCount = 1
        }
        lastTap = now

        if (tapCount >= 2) {
            tapCount = 0
            mateView.expression = MateView.Expression.SURPRISED
            speak("I'm here! What shall we work on?")
        } else {
            mateView.expression = MateView.Expression.HAPPY
            handler.postDelayed({
                if (attached && !dragging) {
                    mateView.expression = MateView.Expression.IDLE
                }
            }, 1200L)
        }
    }

    private fun speak(message: String) {
        if (ttsReady) {
            tts?.speak(
                message,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "jarvis_mate"
            )
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true
            tts?.language = Locale.US
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int = START_NOT_STICKY

    override fun onDestroy() {
        attached = false
        handler.removeCallbacksAndMessages(null)

        if (::windowManager.isInitialized &&
            ::mateView.isInitialized && mateView.parent != null
        ) {
            try {
                windowManager.removeView(mateView)
            } catch (_: Exception) {
            }
        }

        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    class MateView(context: android.content.Context) :
        View(context) {

        enum class Expression { IDLE, HAPPY, SURPRISED }
        enum class Gesture { DOWN, MOVE, UP }

        var expression = Expression.IDLE
            set(value) {
                field = value
                invalidate()
            }

        var onGesture: ((Gesture, Int, Int) -> Boolean)? = null

        private val pixel = Paint().apply {
            isAntiAlias = false
            style = Paint.Style.FILL
        }

        private var downX = 0f
        private var downY = 0f
        private var lastX = 0f
        private var lastY = 0f
        private var startTime = 0L

        private fun block(
            canvas: Canvas,
            x: Float,
            y: Float,
            w: Float,
            h: Float,
            color: Int
        ) {
            pixel.color = color
            canvas.drawRect(x, y, x + w, y + h, pixel)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val sx = width / 58f
            val sy = height / 95f
            canvas.save()
            canvas.scale(sx, sy)

            val t = SystemClock.uptimeMillis() / 1000.0
            val bob = (sin(t * 2.2) * 1.4).toFloat()

            val hair = Color.rgb(130, 57, 43)
            val skin = Color.rgb(255, 211, 190)
            val shirt = Color.rgb(238, 238, 235)
            val coat = Color.rgb(184, 143, 98)
            val dark = Color.rgb(38, 34, 43)
            val eye = Color.rgb(100, 104, 195)
            val red = Color.rgb(178, 40, 55)

            // Pixel-art silhouette and long hair
            block(canvas, 15f, 9f + bob, 28f, 29f, hair)
            block(canvas, 12f, 16f + bob, 8f, 38f, hair)
            block(canvas, 38f, 16f + bob, 8f, 38f, hair)

            // Face
            block(canvas, 19f, 14f + bob, 20f, 23f, skin)

            // Hair fringe
            block(canvas, 17f, 10f + bob, 24f, 8f, hair)
            block(canvas, 17f, 15f + bob, 6f, 7f, hair)
            block(canvas, 24f, 14f + bob, 5f, 7f, hair)
            block(canvas, 32f, 14f + bob, 6f, 5f, hair)

            // Eyes: close briefly to fake blinking
            val blink = (t % 4.0) > 3.84
            if (blink) {
                block(canvas, 21f, 24f + bob, 5f, 1f, dark)
                block(canvas, 32f, 24f + bob, 5f, 1f, dark)
            } else {
                block(canvas, 21f, 22f + bob, 4f, 5f, Color.WHITE)
                block(canvas, 32f, 22f + bob, 4f, 5f, Color.WHITE)
                block(canvas, 22f, 23f + bob, 2f, 3f, eye)
                block(canvas, 33f, 23f + bob, 2f, 3f, eye)
            }

            // Mouth expressions
            when (expression) {
                Expression.IDLE ->
                    block(canvas, 27f, 31f + bob, 4f, 1f, red)
                Expression.HAPPY -> {
                    block(canvas, 26f, 30f + bob, 7f, 2f, red)
                    block(canvas, 27f, 32f + bob, 5f, 1f, red)
                }
                Expression.SURPRISED ->
                    block(canvas, 28f, 30f + bob, 3f, 4f, red)
            }

            // Neck, shirt, tie and coat
            block(canvas, 25f, 37f + bob, 9f, 6f, skin)
            block(canvas, 17f, 40f + bob, 25f, 25f, shirt)
            block(canvas, 13f, 42f + bob, 6f, 29f, coat)
            block(canvas, 40f, 42f + bob, 6f, 29f, coat)
            block(canvas, 27f, 40f + bob, 5f, 18f, red)

            // Arms
            block(canvas, 17f, 48f + bob, 7f, 6f, coat)
            block(canvas, 35f, 48f + bob, 7f, 6f, coat)

            // Shorts and legs
            block(canvas, 19f, 65f + bob, 21f, 10f, dark)
            block(canvas, 21f, 75f + bob, 6f, 14f, skin)
            block(canvas, 31f, 75f + bob, 6f, 14f, skin)

            // Boots
            block(canvas, 19f, 88f + bob, 9f, 5f, dark)
            block(canvas, 30f, 88f + bob, 9f, 5f, dark)

            canvas.restore()

            // Keep the animation running without a continuous full-speed loop
            postInvalidateDelayed(80L)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    lastX = downX
                    lastY = downY
                    startTime = SystemClock.uptimeMillis()
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return onGesture?.invoke(Gesture.DOWN, 0, 0) ?: true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()
                    lastX = event.rawX
                    lastY = event.rawY
                    return onGesture?.invoke(Gesture.MOVE, dx, dy) ?: true
                }

                MotionEvent.ACTION_UP -> {
                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()
                    performClick()
                    return onGesture?.invoke(Gesture.UP, dx, dy) ?: true
                }
            }
            return true
        }

        override fun performClick(): Boolean {
            super.performClick()
            return true
        }
    }
}
