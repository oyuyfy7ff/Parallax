package com.tharunbirla.librecuts.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.tharunbirla.librecuts.models.easeProgress

class KeyframeGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val types = listOf("linear", "ease_in", "ease_out", "ease_in_out", "hold")

    var easingType: String = "linear"
        set(value) {
            field = value
            invalidate()
        }

    var onEasingChange: ((String) -> Unit)? = null

    private val d = resources.displayMetrics.density
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(90, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1f * d
    }
    private val curvePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFB300")
        style = Paint.Style.STROKE
        strokeWidth = 3f * d
        strokeJoin = Paint.Join.ROUND
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pad = 16f * d
        val l = pad
        val t = pad
        val w = width - 2 * pad
        val h = height - 2 * pad
        if (w <= 0f || h <= 0f) return

        canvas.drawRect(l, t, l + w, t + h, gridPaint)
        canvas.drawLine(l + w / 2, t, l + w / 2, t + h, gridPaint)
        canvas.drawLine(l, t + h / 2, l + w, t + h / 2, gridPaint)

        val path = Path()
        if (easingType == "hold") {
            path.moveTo(l, t + h)
            path.lineTo(l + w, t + h)
            path.lineTo(l + w, t)
        } else {
            val steps = 64
            for (i in 0..steps) {
                val p = i / steps.toFloat()
                val x = l + w * p
                val y = t + h * (1f - easeProgress(easingType, p))
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
        }
        canvas.drawPath(path, curvePaint)
        canvas.drawCircle(l, t + h, 5f * d, dotPaint)
        canvas.drawCircle(l + w, t, 5f * d, dotPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) return true
        if (event.action == MotionEvent.ACTION_UP) {
            val next = types[(types.indexOf(easingType) + 1) % types.size]
            easingType = next
            onEasingChange?.invoke(next)
            performClick()
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
