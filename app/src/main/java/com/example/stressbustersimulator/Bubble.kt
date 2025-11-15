package com.example.stressbustersimulator

import android.graphics.Canvas
import android.graphics.Paint

class Bubble(
    var x: Float,
    var y: Float,
    var radius: Float
) {
    var isPopped: Boolean = false

    fun draw(canvas: Canvas, paint: Paint) {
        if (!isPopped) {
            paint.style = Paint.Style.FILL
            canvas.drawCircle(x, y, radius, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            canvas.drawCircle(x, y, radius, paint)
        } else {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            canvas.drawCircle(x, y, radius * 0.7f, paint)
        }
    }

    fun isTouched(touchX: Float, touchY: Float): Boolean {
        val dx = touchX - x
        val dy = touchY - y
        return dx * dx + dy * dy <= radius * radius
    }
}
