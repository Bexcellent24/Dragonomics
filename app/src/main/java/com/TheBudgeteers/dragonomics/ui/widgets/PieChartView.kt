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

    // begin code attribution
    // Custom pie chart rendering logic adapted from:
    // Android Developers, 2023. Draw shapes with Canvas. [online]
    // Available at: <https://developer.android.com/develop/ui/views/graphics/draw> [Accessed 6 October 2025].
    // The approach for using Canvas.drawArc() and RectF bounds to create proportional pie slices
    // was adapted to fit this project’s financial visualisation needs.

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (slices.isEmpty()) return

        val total = slices.sumOf { it.value.toDouble() }.toFloat()
        if (total <= 0f) return

        var startAngle = -90f
        val cx = bounds.centerX()
        val cy = bounds.centerY()
        val radius = bounds.width() / 2f

        for (s in slices) {
            val sweep = (s.value / total) * 360f
            slicePaint.color = s.color
            canvas.drawArc(bounds, startAngle, sweep, true, slicePaint)
            startAngle += sweep
        }


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
        // end code attribution (Android Developers, 2023)

        // begin code attribution
        // Transparent inner circle ("donut hole") effect using PorterDuffXfermode.
        // Adapted from:
        // Android Developers, 2021. Use blending modes with PorterDuffXfermode. [online]
        // Available at: <https://developer.android.com/reference/android/graphics/PorterDuffXfermode> [Accessed 6 October 2025].
        // This clears a circular section in the middle of the chart to create a hollow effect.

        if (holeRadiusPercent > 0f) {
            val eraser = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }
            val saved = canvas.saveLayer(bounds, null)
            canvas.drawCircle(cx, cy, radius * holeRadiusPercent, eraser)
            canvas.restoreToCount(saved)
        }
        // end code attribution (Android Developers, 2021)
    }

    private fun dp(v: Float) = v * resources.displayMetrics.density
}
// reference list
// Android Developers, 2023. Draw shapes with Canvas. [online]
// Available at: <https://developer.android.com/develop/ui/views/graphics/draw> [Accessed 6 October 2025].
// Android Developers, 2021. PorterDuffXfermode (blending modes). [online]
// Available at: <https://developer.android.com/reference/android/graphics/PorterDuffXfermode> [Accessed 6 October 2025].
// Android Developers, 2023. Canvas and graphics overview. [online]
// Available at: <https://developer.android.com/develop/ui/views/graphics> [Accessed 6 October 2025].

