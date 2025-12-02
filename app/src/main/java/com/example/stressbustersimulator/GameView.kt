package com.example.stressbustersimulator

import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import kotlin.random.Random

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {


    enum class Mode {
        BUBBLE,
        PAINT_SPLASH,
        SAND_CUT,
        SOAP_SLICE,
        LEAF_CRUSH,
        ZIPPER_OPEN,
        STICKER_PEEL,
        SHREDDER
    }

    // NEW: Two splash behavior modes
    enum class SplashMode {
        VANISH,        // splash disappears
        PERMANENT      // splash stays forever
    }

    private var splashMode: SplashMode = SplashMode.VANISH

    fun setSplashMode(mode: SplashMode) {
        splashMode = mode
    }

    private var currentMode: Mode = Mode.BUBBLE

    fun setMode(m: Mode) {
        currentMode = m

        when (m) {
            Mode.BUBBLE -> createBubbleGrid()
            Mode.PAINT_SPLASH -> {
                splashes.clear()
                splashCount = 0
                invalidate()
            }
            Mode.SAND_CUT -> {
                sandCuts.clear()
                invalidate()
            }
            else -> {  }
        }
    }


    private val prefs: SharedPreferences =
        context.getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)

    var satisfaction = 0
    var onSatisfactionChange: ((Int) -> Unit)? = null

    private fun updateSatisfaction(amount: Int) {
        satisfaction += amount
        if (satisfaction > 100) satisfaction = 100
        onSatisfactionChange?.invoke(satisfaction)
    }

    private fun updateDailyChallenge() {
        val current = prefs.getInt(Prefs.KEY_DAILY_PROGRESS, 0) + 1
        prefs.edit().putInt(Prefs.KEY_DAILY_PROGRESS, current).apply()

        val goal = prefs.getInt(Prefs.KEY_DAILY_GOAL, 50)

        if (current >= goal && !prefs.getBoolean(Prefs.KEY_UNLOCK_SUPER_SPLASH, false)) {
            unlockTool()
        }
    }

    private fun unlockTool() {
        prefs.edit().putBoolean(Prefs.KEY_UNLOCK_SUPER_SPLASH, true).apply()
        Toast.makeText(context, "Super Splash Unlocked!", Toast.LENGTH_LONG).show()
    }


    private var soundPool: SoundPool? = null
    private var popSoundId = 0
    private var splashSoundId = 0
    private var soundOn = true
    private var vibrationOn = true

    init {
        loadSettings()
        initSound()
    }

    private fun loadSettings() {
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
        soundPool?.release()
        soundPool = null
        super.onDetachedFromWindow()
    }

    private fun playPopSound() {
        if (soundOn) soundPool?.play(popSoundId, 1f, 1f, 1, 0, 1f)
    }

    private fun playSplashSound() {
        if (soundOn) soundPool?.play(splashSoundId, 1f, 1f, 1, 0, 1f)
    }


    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgColor = Color.parseColor("#101010")

    private var screenWidth = 0
    private var screenHeight = 0


    private val bubbles = mutableListOf<Bubble>()
    private var poppedCount = 0
    private var totalBubbles = 0
    private var showResetMessage = false

    private val bubbleColor = Color.parseColor("#74C0FC")
    private val poppedColor = Color.parseColor("#495057")

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

    private fun handleBubblePop(x: Float, y: Float) {
        if (showResetMessage) {
            createBubbleGrid()
            return
        }

        for (b in bubbles) {
            if (!b.isPopped && b.isTouched(x, y)) {
                b.isPopped = true
                poppedCount++

                if (vibrationOn)
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

                playPopSound()
                updateSatisfaction(2)
                updateDailyChallenge()

                break
            }
        }

        if (poppedCount == totalBubbles) showResetMessage = true
        invalidate()
    }


    private val splashes = mutableListOf<Splash>()
    private var splashCount = 0

    private fun handlePaintSplash(x: Float, y: Float) {

        val particleCount = 14

        repeat(particleCount) {

            val angle = Random.nextFloat() * 360f
            val dist = Random.nextFloat() * 100f
            val rad = Math.toRadians(angle.toDouble())

            val sx = x + (dist * Math.cos(rad)).toFloat()
            val sy = y + (dist * Math.sin(rad)).toFloat()
            val radius = (10..45).random().toFloat()

            val color = Color.HSVToColor(
                floatArrayOf(Random.nextFloat() * 360f, 0.85f, 1f)
            )

            val splash = Splash(
                x = sx,
                y = sy,
                radius = radius,
                color = color,
                alpha = 255,
                driftY = (1..4).random().toFloat(),
                driftX = (-2..2).random().toFloat(),
                shrinkRate = Random.nextFloat() * (0.4f - 0.15f) + 0.15f,
                fadeRate = Random.nextInt(5, 12)
            )

            splashes.add(splash)
        }

        playSplashSound()

        if (vibrationOn)
            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

        updateSatisfaction(1)
        updateDailyChallenge()
        invalidate()
    }


    private val sandCuts = mutableListOf<Pair<Float, Float>>()
    private val sandRect = RectF()

    private val cutPaint = Paint().apply {
        color = Color.DKGRAY
        strokeWidth = 8f
    }

    private val sandPaint = Paint().apply {
        color = Color.parseColor("#D2B48C")
        style = Paint.Style.FILL
    }

    private fun handleSandCut(x: Float, y: Float) {
        if (sandRect.contains(x, y)) {
            sandCuts.add(x to y)
            updateSatisfaction(1)
            updateDailyChallenge()
            invalidate()
        }
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        if (event.action == MotionEvent.ACTION_DOWN) {
            when (currentMode) {
                Mode.BUBBLE -> handleBubblePop(x, y)
                Mode.PAINT_SPLASH -> handlePaintSplash(x, y)
                Mode.SAND_CUT -> handleSandCut(x, y)
                else -> { }
            }
        }

        return true
    }


    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(bgColor)

        when (currentMode) {

            Mode.BUBBLE -> {
                for (b in bubbles) {
                    paint.color = if (b.isPopped) poppedColor else bubbleColor
                    b.draw(canvas, paint)
                }
            }

            Mode.PAINT_SPLASH -> {

                val iterator = splashes.iterator()

                while (iterator.hasNext()) {
                    val s = iterator.next()

                    if (splashMode == SplashMode.VANISH) {
                        s.y += s.driftY
                        s.x += s.driftX
                        s.radius -= s.shrinkRate
                        s.alpha -= s.fadeRate
                    }

                    if (splashMode == SplashMode.VANISH &&
                        (s.radius <= 1f || s.alpha <= 5)
                    ) {
                        iterator.remove()
                        continue
                    }

                    paint.color = s.color
                    paint.alpha = s.alpha
                    canvas.drawCircle(s.x, s.y, s.radius, paint)
                }

                paint.alpha = 255
            }

            Mode.SAND_CUT -> {
                canvas.drawRect(sandRect, sandPaint)

                for ((x, y) in sandCuts) {
                    canvas.drawLine(x - 200, y, x + 200, y, cutPaint)
                }
            }

            else -> {}
        }

        invalidate()
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        screenWidth = w
        screenHeight = h

        val left = w * 0.15f
        val right = w * 0.85f
        val top = h * 0.25f
        val bottom = h * 0.8f
        sandRect.set(left, top, right, bottom)

        createBubbleGrid()
    }
}
