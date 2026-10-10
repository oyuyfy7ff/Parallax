package com.tharunbirla.librecuts.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Draggable crosshair that shows where a Null layer currently is on the video frame.
 * Coordinates are relative (0..1) to the visible video rect, same as text/image overlays.
 *
 * While dragging only the view updates; [onCommit] fires once on finger up so the
 * undo stack gets one entry per drag instead of one per move event.
 */
class NullHandleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onCommit: ((relativeX: Float, relativeY: Float) -> Unit)? = null

    private var videoWidth = 0
    private var videoHeight = 0
    private var relX = 0.5f
    private var relY = 0.5f
    private var dragging = false

    private val d = resources.displayMetrics.density
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B388FF")
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * d
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(70, 179, 136, 255)
        style = Paint.Style.FILL
    }
    private val crossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * d
    }

    fun setVideoSize(w: Int, h: Int) {
        videoWidth = w
        videoHeight = h
        invalidate()
    }

    /** Move the handle without notifying [onCommit]. Ignored while the finger is down. */
    fun setPosition(x: Float, y: Float) {
        if (dragging) return
        relX = x
        relY = y
        invalidate()
    }

    private fun getVideoRect(): RectF {
        val rect = RectF()
        if (width <= 0 || height <= 0 || videoWidth <= 0 || videoHeight <= 0) {
            rect.set(0f, 0f, width.toFloat(), height.toFloat())
            return rect
        }
        val containerRatio = width.toFloat() / height
        val videoRatio = videoWidth.toFloat() / videoHeight
        if (videoRatio > containerRatio) {
            val h = width / videoRatio
            val top = (height - h) / 2f
            rect.set(0f, top, width.toFloat(), top + h)
        } else {
            val w = height * videoRatio
            val left = (width - w) / 2f
            rect.set(left, 0f, left + w, height.toFloat())
        }
        return rect
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val r = getVideoRect()
        val cx = r.left + relX * r.width()
        val cy = r.top + relY * r.height()
        val radius = 18f * d
        canvas.drawCircle(cx, cy, radius, fillPaint)
        canvas.drawCircle(cx, cy, radius, ringPaint)
        canvas.drawLine(cx - radius * 0.6f, cy, cx + radius * 0.6f, cy, crossPaint)
        canvas.drawLine(cx, cy - radius * 0.6f, cx, cy + radius * 0.6f, crossPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val r = getVideoRect()
        if (r.width() <= 0f || r.height() <= 0f) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Only grab touches near the handle so the rest of the preview stays usable.
                val cx = r.left + relX * r.width()
                val cy = r.top + relY * r.height()
                val dx = event.x - cx
                val dy = event.y - cy
                val grab = 40f * d
                if (dx * dx + dy * dy > grab * grab) return false
                dragging = true
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging) return false
                relX = ((event.x - r.left) / r.width()).coerceIn(0f, 1f)
                relY = ((event.y - r.top) / r.height()).coerceIn(0f, 1f)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!dragging) return false
                dragging = false
                parent?.requestDisallowInterceptTouchEvent(false)
                if (event.actionMasked == MotionEvent.ACTION_UP) onCommit?.invoke(relX, relY)
                return true
            }
        }
        return false
    }
}
