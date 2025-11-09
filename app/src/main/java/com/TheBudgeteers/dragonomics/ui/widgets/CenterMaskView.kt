package com.TheBudgeteers.dragonomics.ui.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class CenterMaskView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    /** Circle fill colour (defaults to parent background or DeepAbyss-like). */
    var fillColor: Int = Color.parseColor("#0E2A34") // fallback
        set(value) { field = value; paint.color = value; invalidate() }

    /** Size of the circle as % of half the view's width (0..1). */
    var radiusPercent: Float = 0.46f
        set(v) { field = v.coerceIn(0f, 0.9f); invalidate() }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = fillColor
    }
    private val bounds = RectF()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Try to match the container's solid color if possible
        (parent as? View)?.background?.let {
            if (it is ColorDrawable) {
                fillColor = it.color
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val size = min(w, h).toFloat()
        bounds.set(
            (w - size) / 2f, (h - size) / 2f,
            (w + size) / 2f, (h + size) / 2f
        )
    }

    override fun onDraw(canvas: Canvas) {
        val cx = bounds.centerX()
        val cy = bounds.centerY()
        val r = (bounds.width() / 2f) * radiusPercent
        canvas.drawCircle(cx, cy, r, paint)
    }
}
