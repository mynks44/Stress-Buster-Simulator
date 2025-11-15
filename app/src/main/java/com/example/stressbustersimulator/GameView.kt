package com.example.stressbustersimulator

import android.content.Context
import android.graphics.*
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val bgColor = Color.parseColor("#101010")
    private val bubbleColor = Color.parseColor("#74C0FC")
    private val poppedColor = Color.parseColor("#495057")

    private val bubbles = mutableListOf<Bubble>()

    private var screenWidth = 0
    private var screenHeight = 0

    private var poppedCount = 0
    private var totalBubbles = 0
    private var showResetMessage = false

    // Sound
    private var soundPool: SoundPool? = null
    private var popSoundId: Int = 0

    init {
        initSound()
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
        } catch (_: Exception) {
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        soundPool?.release()
        soundPool = null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenWidth = w
        screenHeight = h
        createBubbleGrid()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(bgColor)

        for (bubble in bubbles) {
            paint.color = if (bubble.isPopped) poppedColor else bubbleColor
            bubble.draw(canvas, paint)
        }

        paint.color = Color.WHITE
        paint.textSize = 48f
        paint.style = Paint.Style.FILL
        canvas.drawText("Popped: $poppedCount / $totalBubbles", 40f, 80f, paint)

        if (showResetMessage) {
            paint.textSize = 60f
            val msg = "All popped! Tap to reset"
            val w = paint.measureText(msg)
            canvas.drawText(msg, (screenWidth - w) / 2, screenHeight / 2f, paint)
        }
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

            if (showResetMessage) {
                createBubbleGrid()
                return true
            }

            val x = event.x
            val y = event.y

            var poppedSomething = false

            for (bubble in bubbles) {
                if (!bubble.isPopped && bubble.isTouched(x, y)) {
                    bubble.isPopped = true
                    poppedCount++
                    poppedSomething = true
                    playPopSound()
                    break
                }
            }

            if (poppedSomething) {
                invalidate()
            }

            if (poppedCount == totalBubbles && totalBubbles > 0) {
                showResetMessage = true
                invalidate()
            }
        }
        return true
    }

    private fun playPopSound() {
        soundPool?.play(popSoundId, 1f, 1f, 1, 0, 1f)
    }
}
