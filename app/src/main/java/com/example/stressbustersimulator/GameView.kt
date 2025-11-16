package com.example.stressbustersimulator

import android.content.Context
import android.graphics.*
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.sqrt
import kotlin.random.Random

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    enum class Mode {
        BUBBLE,
        PAINT_SPLASH
    }

    private var currentMode: Mode = Mode.BUBBLE

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgColor = Color.parseColor("#101010")

    private val bubbleColor = Color.parseColor("#74C0FC")
    private val poppedColor = Color.parseColor("#495057")

    private var screenWidth = 0
    private var screenHeight = 0

    private val bubbles = mutableListOf<Bubble>()
    private var poppedCount = 0
    private var totalBubbles = 0
    private var showResetMessage = false

    private val splashes = mutableListOf<Splash>()
    private var splashCount = 0

    private var soundPool: SoundPool? = null
    private var popSoundId: Int = 0
    private var splashSoundId: Int = 0

    private var soundOn = true
    private var vibrationOn = true

    private val modeBtnRadius = 60f
    private val modeBtnMargin = 40f
    private var modeBtnCenterX = 0f
    private var modeBtnCenterY = 0f

    init {
        initSound()
        loadSettings()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        loadSettings()
    }

    private fun loadSettings() {
        val prefs = context.getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)
        soundOn = prefs.getBoolean(Prefs.KEY_SOUND, true)
        vibrationOn = prefs.getBoolean(Prefs.KEY_VIBRATION, true)
    }

    private fun initSound() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setAudioAttributes(attrs)
            .setMaxStreams(3)
            .build()

        try {
            popSoundId = soundPool!!.load(context, R.raw.pop, 1)
            splashSoundId = soundPool!!.load(context, R.raw.splash, 1)
        } catch (_: Exception) { }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        soundPool?.release()
        soundPool = null
    }

    fun setMode(mode: Mode) {
        currentMode = mode

        when (mode) {
            Mode.BUBBLE -> createBubbleGrid()
            Mode.PAINT_SPLASH -> {
                splashes.clear()
                splashCount = 0
                invalidate()
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenWidth = w
        screenHeight = h

        modeBtnCenterX = w - modeBtnMargin - modeBtnRadius
        modeBtnCenterY = modeBtnMargin + modeBtnRadius

        createBubbleGrid()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(bgColor)

        when (currentMode) {
            Mode.BUBBLE -> drawBubbleMode(canvas)
            Mode.PAINT_SPLASH -> drawPaintMode(canvas)
        }

        drawModeButton(canvas)
    }

    private fun drawBubbleMode(canvas: Canvas) {
        for (b in bubbles) {
            paint.color = if (b.isPopped) poppedColor else bubbleColor
            b.draw(canvas, paint)
        }

        paint.color = Color.WHITE
        paint.textSize = 48f
        canvas.drawText("Mode: Bubble Wrap", 40f, 80f, paint)
        canvas.drawText("Popped: $poppedCount / $totalBubbles", 40f, 140f, paint)

        if (showResetMessage) {
            paint.textSize = 60f
            val msg = "All popped! Tap to reset"
            val w = paint.measureText(msg)
            canvas.drawText(msg, (screenWidth - w) / 2, screenHeight / 2f, paint)
        }
    }

    private fun drawPaintMode(canvas: Canvas) {
        for (s in splashes) {
            paint.style = Paint.Style.FILL
            paint.color = s.color
            paint.alpha = s.alpha
            canvas.drawCircle(s.x, s.y, s.radius, paint)
        }

        paint.color = Color.WHITE
        paint.textSize = 48f
        paint.alpha = 255
        canvas.drawText("Mode: Paint Splash", 40f, 80f, paint)
        canvas.drawText("Splashes: $splashCount", 40f, 140f, paint)
    }

    private fun drawModeButton(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#222222")
        canvas.drawCircle(modeBtnCenterX, modeBtnCenterY, modeBtnRadius, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = Color.WHITE
        canvas.drawCircle(modeBtnCenterX, modeBtnCenterY, modeBtnRadius, paint)

        paint.style = Paint.Style.FILL
        paint.textSize = 32f
        val label = if (currentMode == Mode.BUBBLE) "Bub" else "Paint"
        val tw = paint.measureText(label)
        canvas.drawText(label, modeBtnCenterX - tw / 2, modeBtnCenterY + 12f, paint)
    }

    private fun createBubbleGrid() {
        bubbles.clear()
        poppedCount = 0
        showResetMessage = false

        if (screenWidth == 0 || screenHeight == 0) return

        val cols = 8
        val rows = 12

        val padding = 40f
        val availableWidth = screenWidth - padding * 2
        val availableHeight = screenHeight - padding * 3

        val radius = (availableWidth / (cols * 2))
            .coerceAtMost(availableHeight / (rows * 2)) * 0.8f

        val xStep = availableWidth / cols
        val yStep = availableHeight / rows

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val x = padding + xStep * c + xStep / 2
                val y = padding * 2 + yStep * r + yStep / 2
                bubbles.add(Bubble(x, y, radius))
            }
        }

        totalBubbles = bubbles.size
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {

            val x = event.x
            val y = event.y

            if (isInModeButton(x, y)) {
                switchMode()
                invalidate()
                return true
            }

            when (currentMode) {
                Mode.BUBBLE -> handleBubbleTouch(x, y)
                Mode.PAINT_SPLASH -> handlePaintTouch(x, y)
            }
        }
        return true
    }

    private fun isInModeButton(x: Float, y: Float): Boolean {
        val dx = x - modeBtnCenterX
        val dy = y - modeBtnCenterY
        return sqrt(dx * dx + dy * dy) <= modeBtnRadius
    }

    private fun switchMode() {
        currentMode =
            if (currentMode == Mode.BUBBLE) Mode.PAINT_SPLASH else Mode.BUBBLE
        setMode(currentMode)
    }

    private fun handleBubbleTouch(x: Float, y: Float) {
        if (showResetMessage) {
            createBubbleGrid()
            return
        }

        for (b in bubbles) {
            if (!b.isPopped && b.isTouched(x, y)) {
                b.isPopped = true
                poppedCount++

                // 5.3 Haptic feedback
                if (vibrationOn)
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

                playPopSound()
                break
            }
        }

        invalidate()

        if (poppedCount == totalBubbles && totalBubbles > 0) {
            showResetMessage = true
        }
    }

    private fun handlePaintTouch(x: Float, y: Float) {
        val splashNum = 8

        for (i in 0 until splashNum) {
            val angle = Random.nextFloat() * 360f
            val dist = Random.nextFloat() * 80f
            val rad = Math.toRadians(angle.toDouble())

            val sx = x + (dist * Math.cos(rad)).toFloat()
            val sy = y + (dist * Math.sin(rad)).toFloat()
            val radius = Random.nextFloat() * 30f + 10f

            val color = Color.HSVToColor(
                255,
                floatArrayOf(Random.nextFloat() * 360f, 0.8f, 1.0f)
            )

            splashes.add(Splash(sx, sy, radius, color))
            splashCount++
        }

        playSplashSound()

        if (vibrationOn)
            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

        invalidate()
    }

    private fun playPopSound() {
        if (!soundOn) return
        soundPool?.play(popSoundId, 1f, 1f, 1, 0, 1f)
    }

    private fun playSplashSound() {
        if (!soundOn) return
        soundPool?.play(splashSoundId, 1f, 1f, 1, 0, 1f)
    }
}
