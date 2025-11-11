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
        color = Color.parseColor("#DDDDDD")
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

    private var savingsColor: Int = Color.parseColor("#2ECC71") // base "current/income" segment
    private var endCapColor: Int = Color.parseColor("#FFF5A5")
    private var markerColor: Int = endCapColor

    private var trackMatchesParentBg: Boolean = true

    // begin code attribution
    // Loading a custom Typeface via ResourcesCompat.getFont adapted from:
    // Android Developers, 2023. Fonts in XML / programmatic access. [online]
    // Available at: <https://developer.android.com/guide/topics/ui/look-and-feel/fonts-in-xml> [Accessed 6 October 2025].
    init {
        ResourcesCompat.getFont(context, R.font.aref_ruqaa)?.let { tf ->
            textPaint.typeface = tf
        }
        val gold = ContextCompat.getColor(context, R.color.GoldenEmber)
        endCapColor = gold
        markerColor = gold
    }
    // end code attribution (Android Developers, 2023)

    private fun updateTrackColorFromParent() {
        val parentView = parent as? View
        val bgColor = (parentView?.background as? ColorDrawable)?.color
        trackPaint.color = bgColor ?: Color.TRANSPARENT
    }

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

    // begin code attribution
    // Measuring a custom view with onMeasure/resolveSize pattern adapted from:
    // Android Developers, 2024. Custom view components: onMeasure. [online]
    // Available at: <https://developer.android.com/training/custom-views/custom-drawing> [Accessed 6 October 2025].
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minH = (dp(56f)).roundToInt()
        val desiredH = resolveSize(minH, heightMeasureSpec)
        val desiredW = resolveSize(suggestedMinimumWidth, widthMeasureSpec)
        setMeasuredDimension(desiredW, desiredH)
    }
    // end code attribution (Android Developers, 2024)

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

        canvas.drawRoundRect(outer, corner, corner, trackPaint)

        if (maxGoal <= 0.0) {
            canvas.drawRoundRect(outer, corner, corner, borderPaint)
            drawLabels(canvas, outer, 0f, outer.right, 0.0)
            return
        }

        val bw = borderPaint.strokeWidth
        val inner = RectF(outer).apply { inset(bw, bw) }
        val innerCorner = (corner - bw).coerceAtLeast(0f)

        fun xAbs(value: Double): Float {
            val clamped = value.coerceIn(0.0, maxGoal)
            val ratio = (clamped / maxGoal).toFloat()
            return inner.left + ratio * inner.width()
        }

        val current01 = (totalIncome / maxGoal).coerceIn(0.0, 1.0)
        val currentW = (current01 * inner.width()).toFloat()
        val currentRect = RectF(inner.left, inner.top, inner.left + currentW, inner.bottom)

        canvas.save()
        Path().apply { addRoundRect(inner, innerCorner, innerCorner, Path.Direction.CW) }
            .also { canvas.clipPath(it) }

        if (currentRect.width() > 0f) {
            segmentPaint.color = savingsColor
            canvas.drawRect(currentRect, segmentPaint)
        }

        val expFracOfCurrent =
            if (totalIncome > 0.0) (totalExpenses / totalIncome).coerceIn(0.0, 1.0) else 0.0
        val expenseTotalW = (currentRect.width() * expFracOfCurrent).toFloat()

        if (expenseTotalW > 0f && currentRect.width() > 0f) {
            canvas.save()
            canvas.clipRect(currentRect)

            val items = segments.filter { it.amount > 0.0 }
            val minSegPx = dp(2f)
            val totalForSplit = items.sumOf { it.amount }.coerceAtLeast(1e-6)

            var cursor = currentRect.left

            for (seg in items) {
                val rawW = (expenseTotalW * (seg.amount / totalForSplit)).toFloat()
                val segW = max(minSegPx, rawW) // keep tiny segments visible
                val next = min(cursor + segW, currentRect.right)

                if (next > cursor) {
                    segmentPaint.color = seg.color
                    canvas.drawRect(cursor, currentRect.top, next, currentRect.bottom, segmentPaint)
                    cursor = next
                    if (cursor >= currentRect.right) break
                }
            }
            canvas.restore()
        }

        canvas.restore()

        val minMarkerW = dp(8f)
        val maxCapW = dp(8f)
        val gutter = dp(24f)

        val minRaw = xAbs(minGoal)
        val maxRaw = inner.right // END of bar

        val minMarkerX = if (minRaw < inner.left + gutter) inner.left + gutter else minRaw
        val maxMarkerX = maxRaw

        markerPaint.color = markerColor
        canvas.drawRect(
            minMarkerX - minMarkerW / 2f, inner.top,
            minMarkerX + minMarkerW / 2f, inner.bottom,
            markerPaint
        )

        markerPaint.color = endCapColor
        canvas.drawRect(
            inner.right - maxCapW, inner.top,
            inner.right, inner.bottom,
            markerPaint
        )

        canvas.drawRoundRect(outer, corner, corner, borderPaint)

        val minLabelX = max(minRaw, inner.left + gutter)
        val maxLabelX = min(maxRaw, inner.right - gutter)
        drawLabels(canvas, outer, minLabelX, maxLabelX, maxGoal)
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

        textPaint.textSize = sp(14f)
        textPaint.typeface = boldTf

        if (maxGoalVal > 0) {
            val gutter = dp(24f)
            val safeLeft  = bar.left + gutter
            val safeRight = bar.right - gutter

            // ----- Min Goal -----
            run {
                val title = "Min Goal"
                val value = "${minGoal.roundToInt()}"

                val useLeftAlign = minX < safeLeft + dp(8f)
                val x = if (useLeftAlign) safeLeft else minX

                textPaint.textAlign = if (useLeftAlign) Paint.Align.LEFT else Paint.Align.CENTER
                canvas.drawText(title, x, yAbove - dp(10f), textPaint)
                canvas.drawText(value, x, yAbove + dp(6f), textPaint)
            }

            // ----- Max Goal -----
            run {
                val title = "Max Goal"
                val value = "${maxGoal.roundToInt()}"

                val useRightAlign = maxX > safeRight - dp(8f)
                val x = if (useRightAlign) safeRight else maxX - dp(6f)

                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText(title, x, yAbove - dp(10f), textPaint)
                canvas.drawText(value, x, yAbove + dp(6f), textPaint)
            }
        } else {
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Set goals in Profile", bar.right - dp(6f), yAbove, textPaint)
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
    // begin code attribution
    // Density/scaled-density helpers (dp/sp) based on DisplayMetrics guidance adapted from:
    // Android Developers, 2024. Support different pixel densities. [online]
    // Available at: <https://developer.android.com/training/multiscreen/screendensities> [Accessed 6 October 2025].
    private fun dp(v: Float) = v * resources.displayMetrics.density
    private fun sp(v: Float) = v * resources.displayMetrics.scaledDensity
    // end code attribution (Android Developers, 2024)

// reference list
// Android Developers, 2023. Draw with Canvas. [online]
// Available at: <https://developer.android.com/develop/ui/views/graphics/draw> [Accessed 6 October 2025].
// Android Developers, 2020. Canvas clip operations. [online]
// Available at: <https://developer.android.com/reference/android/graphics/Canvas#clipPath(android.graphics.Path)> [Accessed 6 October 2025].
// Android Developers, 2023. Fonts in XML / programmatic access. [online]
// Available at: <https://developer.android.com/guide/topics/ui/look-and-feel/fonts-in-xml> [Accessed 6 October 2025].
// Android Developers, 2024. Custom view components: onMeasure. [online]
// Available at: <https://developer.android.com/training/custom-views/custom-drawing> [Accessed 6 October 2025].
// Android Developers, 2024. Support different pixel densities. [online]
// Available at: <https://developer.android.com/training/multiscreen/screendensities> [Accessed 6 October 2025].

}
