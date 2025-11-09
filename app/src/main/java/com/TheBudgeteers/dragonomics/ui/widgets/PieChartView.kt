package com.TheBudgeteers.dragonomics.ui.widgets

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class PieChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    // Use Float for values to match Canvas APIs nicely
    data class Slice(val label: String, val value: Float, val color: Int)

    private val slicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#CFA658") // Golden divider
        strokeWidth = dp(2f)
    }

    private val bounds = RectF()
    private var slices: List<Slice> = emptyList()

    // Donut hole (percentage of radius). 0 = full pie, 0.60 = big hole
    var holeRadiusPercent: Float = 0.52f
        set(v) { field = v.coerceIn(0f, 0.9f); invalidate() }

    fun setData(newSlices: List<Slice>) {
        slices = newSlices.filter { it.value > 0f }
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val size = min(w, h)
        val pad = dp(6f)
        bounds.set(
            (w - size) / 2f + pad,
            (h - size) / 2f + pad,
            (w + size) / 2f - pad,
            (h + size) / 2f - pad
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (slices.isEmpty()) return

        val total = slices.sumOf { it.value.toDouble() }.toFloat()
        if (total <= 0f) return

        var startAngle = -90f
        val cx = bounds.centerX()
        val cy = bounds.centerY()
        val radius = bounds.width() / 2f

        // Draw filled slices
        for (s in slices) {
            val sweep = (s.value / total) * 360f
            slicePaint.color = s.color
            canvas.drawArc(bounds, startAngle, sweep, true, slicePaint)
            startAngle += sweep
        }

        // Draw thin radial gold dividers
        startAngle = -90f
        for (s in slices) {
            val sweep = (s.value / total) * 360f

            val angleRad = Math.toRadians(startAngle.toDouble())
            val r = radius.toDouble()
            val x = (cx.toDouble() + r * cos(angleRad)).toFloat()
            val y = (cy.toDouble() + r * sin(angleRad)).toFloat()

            canvas.drawLine(cx, cy, x, y, dividerPaint)
            startAngle += sweep
        }

        // Draw donut hole to let your flame ring & labels read nicely
        if (holeRadiusPercent > 0f) {
            val eraser = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }
            val saved = canvas.saveLayer(bounds, null)
            canvas.drawCircle(cx, cy, radius * holeRadiusPercent, eraser)
            canvas.restoreToCount(saved)
        }
    }

    private fun dp(v: Float) = v * resources.displayMetrics.density
}
