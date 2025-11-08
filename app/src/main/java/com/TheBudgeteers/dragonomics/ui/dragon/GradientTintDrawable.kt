package com.TheBudgeteers.dragonomics.utilities

import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.DrawableCompat

/**
 * A drawable wrapper that applies a vertical( can change it if need be) gradient tint overlay to any drawable.
 * Uses MULTIPLY to colorize while preserving details.
 * Only applies gradient to non-transparent pixels.
 *
 */
class GradientTintDrawable(
    private val drawable: Drawable,
    private val topColor: Int,
    private val bottomColor: Int
) : Drawable(), Drawable.Callback {

    private val wrappedDrawable = DrawableCompat.wrap(drawable.mutate()).apply {
        callback = this@GradientTintDrawable
    }

    override fun draw(canvas: Canvas) {
        val width = bounds.width()
        val height = bounds.height()

        if (width <= 0 || height <= 0) return

        // Create gradient shader
        val gradient = LinearGradient(
            0f, bounds.top.toFloat(),
            0f, bounds.bottom.toFloat(),
            topColor, bottomColor,
            Shader.TileMode.CLAMP
        )

        // Save a layer for proper compositing with transparency
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val saveCount = canvas.saveLayer(
            bounds.left.toFloat(),
            bounds.top.toFloat(),
            bounds.right.toFloat(),
            bounds.bottom.toFloat(),
            paint
        )

        // Draw the base dragon first
        wrappedDrawable.draw(canvas)

        // Apply gradient using MULTIPLY mode
        // This colorizes white→gradient colors while preserving grey details
        paint.shader = gradient
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)

        // Draw gradient - MULTIPLY only affects non-transparent pixels
        canvas.drawRect(bounds, paint)

        canvas.restoreToCount(saveCount)
    }

    override fun setAlpha(alpha: Int) {
        wrappedDrawable.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        // Intentionally ignore - we're applying our own gradient
    }

    @Deprecated("Deprecated in Java") // Needed, just telling kotlin to shut up and that I know its deprecated
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        super.setBounds(left, top, right, bottom)
        wrappedDrawable.setBounds(left, top, right, bottom)
    }

    override fun getIntrinsicWidth(): Int = wrappedDrawable.intrinsicWidth
    override fun getIntrinsicHeight(): Int = wrappedDrawable.intrinsicHeight

    // Support for animated drawables
    override fun invalidateDrawable(who: Drawable) {
        invalidateSelf()
    }

    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
        scheduleSelf(what, `when`)
    }

    override fun unscheduleDrawable(who: Drawable, what: Runnable) {
        unscheduleSelf(what)
    }
}

// Reference:
// Android Developers, 2025. Custom Drawables. [online] Available at: <https://developer.android.com/guide/topics/graphics/drawables> [Accessed 8 November 2025].
// Stack Overflow, 2024. PorterDuff modes explained. [online] Available at: <https://stackoverflow.com/questions/8280027> [Accessed 8 November 2025].