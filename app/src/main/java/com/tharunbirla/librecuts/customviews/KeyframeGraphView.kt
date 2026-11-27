package com.tharunbirla.librecuts.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import com.tharunbirla.librecuts.models.bezierEasing
import com.tharunbirla.librecuts.models.easeProgress
import com.tharunbirla.librecuts.models.easingHandles
import kotlin.math.hypot

/**
 * Easing curve editor for the keyframe at the playhead: drag the two handles to shape a cubic-bezier
 * curve. Handles follow the finger at reduced speed for fine control; the change is committed on release.
 */
class KeyframeGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var easingType: String = "linear"
        set(value) {
            field = value
            handles = easingHandles(value)
            invalidate()
        }

    var onEasingChange: ((String) -> Unit)? = null

    /** Apply a preset (or any easing string) as if the user had edited it. */
    fun applyPreset(type: String) {
        easingType = type
        onEasingChange?.invoke(type)
    }

    private var handles: FloatArray? = easingHandles("linear")
    private var dragging = -1
    private var lastX = 0f
    private var lastY = 0f

    // Curve space shown: x 0..1, y -0.6..1.6 so overshoot/anticipation curves stay visible.
    private val yMin = -0.6f
    private val yMax = 1.6f
    private val precision = 0.25f // handle moves a quarter as far as the finger
    private var downX = 0f
    private var downY = 0f

    private val d = resources.displayMetrics.density
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(60, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1f * d
    }
    private val framePaint = Paint(gridPaint).apply { color = Color.argb(120, 255, 255, 255) }
    private val curvePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFB300")
        style = Paint.Style.STROKE
        strokeWidth = 3f * d
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val armPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(170, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * d
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFB300")
        style = Paint.Style.FILL
    }
    private val handleRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f * d
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 255, 255, 255)
        textSize = 11f * d
    }

    private val padH get() = 20f * d
    private val padV get() = 14f * d
    private fun gx(x: Float) = padH + x * (width - 2 * padH)
    private fun gy(y: Float) = padV + (yMax - y) / (yMax - yMin) * (height - 2 * padV)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 2 * padH || height <= 2 * padV) return

        // Grid: quarters across, plus the 0 and 1 value lines.
        for (i in 1..3) canvas.drawLine(gx(i / 4f), gy(yMax), gx(i / 4f), gy(yMin), gridPaint)
        canvas.drawLine(gx(0f), gy(0f), gx(1f), gy(0f), framePaint)
        canvas.drawLine(gx(0f), gy(1f), gx(1f), gy(1f), framePaint)
        canvas.drawLine(gx(0f), gy(0.5f), gx(1f), gy(0.5f), gridPaint)

        val path = Path()
        val h = handles
        if (h == null) {
            path.moveTo(gx(0f), gy(0f))
            path.lineTo(gx(1f), gy(0f))
            path.lineTo(gx(1f), gy(1f))
        } else {
            val steps = 120
            for (i in 0..steps) {
                val p = i / steps.toFloat()
                val x = gx(p)
                val y = gy(easeProgress(easingType, p))
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
        }
        canvas.drawPath(path, curvePaint)
        canvas.drawCircle(gx(0f), gy(0f), 4f * d, dotPaint)
        canvas.drawCircle(gx(1f), gy(1f), 4f * d, dotPaint)

        if (h != null) {
            canvas.drawLine(gx(0f), gy(0f), gx(h[0]), gy(h[1]), armPaint)
            canvas.drawLine(gx(1f), gy(1f), gx(h[2]), gy(h[3]), armPaint)
            for (i in 0..1) {
                val r = (if (dragging == i) 10f else 8f) * d
                canvas.drawCircle(gx(h[i * 2]), gy(h[i * 2 + 1]), r, handlePaint)
                canvas.drawCircle(gx(h[i * 2]), gy(h[i * 2 + 1]), r, handleRing)
            }
            val label = String.format(java.util.Locale.US, "%.2f, %.2f · %.2f, %.2f", h[0], h[1], h[2], h[3])
            canvas.drawText(label, width - padH - labelPaint.measureText(label), padV + labelPaint.textSize, labelPaint)
        } else {
            canvas.drawText("Hold", width - padH - labelPaint.measureText("Hold"), padV + labelPaint.textSize, labelPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val h = handles ?: return super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val d0 = hypot(event.x - gx(h[0]), event.y - gy(h[1]))
                val d1 = hypot(event.x - gx(h[2]), event.y - gy(h[3]))
                val reach = 36f * d
                dragging = when {
                    d0 <= reach && d0 <= d1 -> 0
                    d1 <= reach -> 1
                    else -> TAP
                }
                downX = event.x
                downY = event.y
                if (dragging == TAP) return true
                lastX = event.x
                lastY = event.y
                parent?.requestDisallowInterceptTouchEvent(true)
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragging == -1) return false
                if (dragging == TAP) return true
                val dx = (event.x - lastX) / (width - 2 * padH) * precision
                val dy = -(event.y - lastY) / (height - 2 * padV) * (yMax - yMin) * precision
                lastX = event.x
                lastY = event.y
                val i = dragging * 2
                h[i] = (h[i] + dx).coerceIn(0f, 1f)
                h[i + 1] = (h[i + 1] + dy).coerceIn(yMin, yMax)
                // Redraw from the edited handles without committing yet.
                setCurve(bezierEasing(h[0], h[1], h[2], h[3]), h)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging == -1) return false
                if (dragging == TAP) {
                    dragging = -1
                    val slop = android.view.ViewConfiguration.get(context).scaledTouchSlop
                    if (event.actionMasked == MotionEvent.ACTION_UP && hypot(event.x - downX, event.y - downY) < slop) {
                        performClick()
                        showNumberEditor(h)
                    }
                    return true
                }
                dragging = -1
                parent?.requestDisallowInterceptTouchEvent(false)
                val type = bezierEasing(h[0], h[1], h[2], h[3])
                setCurve(type, h)
                if (event.actionMasked == MotionEvent.ACTION_UP) {
                    onEasingChange?.invoke(type)
                    performClick()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    /** Update the curve while keeping the live handle array (avoids re-parsing rounding during a drag). */
    private fun setCurve(type: String, h: FloatArray) {
        easingType = type
        handles = h
        invalidate()
    }

    /** Type exact handle values (x 0..1, y -0.6..1.6), like CSS cubic-bezier(). */
    private fun showNumberEditor(h: FloatArray) {
        val labels = listOf("Handle 1 time (x)", "Handle 1 value (y)", "Handle 2 time (x)", "Handle 2 value (y)")
        val inputs = labels.mapIndexed { i, label ->
            com.google.android.material.textfield.TextInputEditText(context).apply {
                hint = label
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
                setText(String.format(java.util.Locale.US, "%.3f", h[i]))
                setSelectAllOnFocus(true)
            }
        }
        val pad = (20 * d).toInt()
        val form = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
            inputs.forEach { addView(it) }
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
            .setTitle("Easing curve")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Set") { _, _ ->
                val v = inputs.mapIndexed { i, e -> e.text?.toString()?.replace(',', '.')?.toFloatOrNull()?.takeIf { it.isFinite() } ?: h[i] }
                applyPreset(bezierEasing(v[0].coerceIn(0f, 1f), v[1].coerceIn(yMin, yMax), v[2].coerceIn(0f, 1f), v[3].coerceIn(yMin, yMax)))
            }
            .show()
    }

    private companion object {
        const val TAP = -2
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
