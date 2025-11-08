package com.TheBudgeteers.dragonomics.ui.widgets

import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.TheBudgeteers.dragonomics.R
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class GoalBarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Segment(val amount: Double, val color: Int)

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#DDDDDD") // will be replaced when matching parent bg
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
        color = Color.parseColor("#222222")
    }
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sp(12f)
        color = Color.BLACK
    }
    private val segmentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private var corner = dp(8f)
    private var contentPad = dp(8f)
    private var labelSpaceTop = dp(32f)

    private var maxGoal = 0.0
    private var minGoal = 0.0
    private var totalExpenses = 0.0
    private var totalIncome = 0.0
    private var segments: List<Segment> = emptyList()

    private var savingsColor: Int = Color.parseColor("#2ECC71")
    private var endCapColor: Int = Color.parseColor("#FFF5A5")
    private var markerColor: Int = endCapColor

    // NEW: make the track blend with the parent background
    private var trackMatchesParentBg: Boolean = true

    init {
        ResourcesCompat.getFont(context, R.font.aref_ruqaa)?.let { tf ->
            textPaint.typeface = tf
        }
        val gold = ContextCompat.getColor(context, R.color.GoldenEmber)
        endCapColor = gold
        markerColor = gold
    }

    /** Try to copy the parent view's background color for the track. */
    private fun updateTrackColorFromParent() {
        val parentView = parent as? View
        val bgColor = (parentView?.background as? ColorDrawable)?.color
        trackPaint.color = bgColor ?: Color.TRANSPARENT
    }

    /** Public toggle if you ever want to revert to a solid track. */
    fun setTrackMatchesParentBackground(enabled: Boolean) {
        trackMatchesParentBg = enabled
        if (enabled) updateTrackColorFromParent()
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (trackMatchesParentBg) updateTrackColorFromParent()
    }

    fun setData(
        maxGoal: Double,
        minGoal: Double,
        expenseSegments: List<Segment>,
        totalIncome: Double
    ) {
        this.maxGoal = max(0.0, maxGoal)
        this.minGoal = minGoal.coerceIn(0.0, this.maxGoal)
        this.segments = expenseSegments
        this.totalIncome = max(0.0, totalIncome)
        this.totalExpenses = expenseSegments.sumOf { it.amount }.coerceAtLeast(0.0)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minH = (dp(56f)).roundToInt()
        val desiredH = resolveSize(minH, heightMeasureSpec)
        val desiredW = resolveSize(suggestedMinimumWidth, widthMeasureSpec)
        setMeasuredDimension(desiredW, desiredH)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (trackMatchesParentBg) updateTrackColorFromParent()

        val w = width.toFloat()
        val h = height.toFloat()

        val left = contentPad
        val top = contentPad + labelSpaceTop
        val right = w - contentPad
        val bottom = h - contentPad
        val outer = RectF(left, top, right, bottom)

        // Track background (under everything)
        canvas.drawRoundRect(outer, corner, corner, trackPaint)

        if (maxGoal <= 0.0) {
            canvas.drawRoundRect(outer, corner, corner, borderPaint)
            drawLabels(canvas, outer, 0f, outer.right, 0.0)
            return
        }

        // Inner clip rect to prevent segment bleed beyond rounded corners
        val bw = borderPaint.strokeWidth
        val inner = RectF(outer).apply { inset(bw, bw) }
        val innerCorner = (corner - bw).coerceAtLeast(0f)

        val usableW = inner.width()
        fun xFor(value: Double): Float {
            val clamped = value.coerceIn(0.0, maxGoal)
            val ratio = (clamped / maxGoal).toFloat()
            return inner.left + ratio * usableW
        }

        val remaining = max(0.0, totalIncome - totalExpenses)
        val remainRight = xFor(remaining)
        val remainRect = RectF(inner.left, inner.top, remainRight, inner.bottom)

        canvas.save()
        val clipPath = Path().apply { addRoundRect(inner, innerCorner, innerCorner, Path.Direction.CW) }
        canvas.clipPath(clipPath)

        if (remaining > 0.0 && remainRect.width() > 0f) {
            var cursor = remainRect.left
            val remainingWidth = remainRect.width()
            val safeRemaining = max(remaining, 1e-6)

            segments.forEach { seg ->
                if (seg.amount <= 0.0) return@forEach
                val segW = (seg.amount / safeRemaining).toFloat().coerceAtMost(1f) * remainingWidth
                val next = min(cursor + segW, remainRect.right)
                if (next > cursor) {
                    segmentPaint.color = seg.color
                    canvas.drawRect(cursor, remainRect.top, next, remainRect.bottom, segmentPaint)
                    cursor = next
                    if (cursor >= remainRect.right) return@forEach
                }
            }

            if (cursor < remainRect.right) {
                segmentPaint.color = savingsColor
                canvas.drawRect(cursor, remainRect.top, remainRect.right, remainRect.bottom, segmentPaint)
            }
        }
        canvas.restore()

        // Min / Max markers (inside inner rect)
        val minX = xFor(minGoal)
        val maxX = inner.right
        val minMarkerW = dp(8f)
        val maxCapW = dp(8f)

        markerPaint.color = markerColor
        canvas.drawRect(minX - minMarkerW / 2f, inner.top, minX + minMarkerW / 2f, inner.bottom, markerPaint)

        markerPaint.color = endCapColor
        canvas.drawRect(maxX - maxCapW, inner.top, maxX, inner.bottom, markerPaint)

        // Border on top
        canvas.drawRoundRect(outer, corner, corner, borderPaint)

        // Labels (use OUTER for positioning above)
        drawLabels(canvas, outer, minX, maxX, maxGoal)
    }

    private fun drawLabels(
        canvas: Canvas,
        bar: RectF,
        minX: Float,
        maxX: Float,
        maxGoalVal: Double
    ) {
        val yAbove = bar.top - dp(10f)

        val oldSize = textPaint.textSize
        val oldTf = textPaint.typeface
        val boldTf = Typeface.create(oldTf, Typeface.BOLD)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = sp(14f)
        textPaint.typeface = boldTf

        if (maxGoalVal > 0) {
            canvas.drawText("Min Goal", minX, yAbove - dp(10f), textPaint)
            canvas.drawText("${minGoal.roundToInt()}", minX, yAbove + dp(6f), textPaint)

            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Max Goal", maxX - dp(6f), yAbove - dp(10f), textPaint)
            canvas.drawText("${maxGoal.roundToInt()}", maxX - dp(6f), yAbove + dp(6f), textPaint)
        } else {
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Set goals in Profile", maxX, yAbove, textPaint)
        }

        textPaint.textSize = oldSize
        textPaint.typeface = oldTf
    }

    fun setThemeColors(trackColor: Int, borderColor: Int, labelColor: Int, endCapColor: Int) {
        trackPaint.color  = trackColor
        borderPaint.color = borderColor
        textPaint.color   = labelColor
        this.endCapColor  = endCapColor
        this.markerColor  = endCapColor
        invalidate()
    }

    private fun dp(v: Float) = v * resources.displayMetrics.density
    private fun sp(v: Float) = v * resources.displayMetrics.scaledDensity
}
