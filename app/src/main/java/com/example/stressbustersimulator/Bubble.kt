package com.example.stressbustersimulator

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.sqrt

class Bubble(
    var x: Float,
    var y: Float,
    var radius: Float
) {
    var isPopped = false
    private var popProgress = 0f

    fun draw(canvas: Canvas, paint: Paint) {

        if (!isPopped) {
            paint.color = Color.argb(60, 180, 220, 255)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(x, y, radius * 1.15f, paint)

            paint.color = Color.argb(255, 116, 192, 252)
            canvas.drawCircle(x, y, radius, paint)

            paint.color = Color.WHITE
            paint.style = Paint.Style.FILL
            canvas.drawCircle(x - radius * 0.3f, y - radius * 0.3f, radius * 0.25f, paint)

            paint.style = Paint.Style.STROKE
            paint.color = Color.WHITE
            paint.strokeWidth = 4f
            canvas.drawCircle(x, y, radius, paint)

        } else {
            popProgress += 0.07f
            if (popProgress < 1f) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                paint.color = Color.argb((255 * (1 - popProgress)).toInt(), 255, 255, 255)

                canvas.drawCircle(x, y, radius * (0.7f + popProgress * 1.2f), paint)
            }
        }
    }

    fun isTouched(tx: Float, ty: Float): Boolean {
        val dx = tx - x
        val dy = ty - y
        return dx * dx + dy * dy <= radius * radius
    }
}
