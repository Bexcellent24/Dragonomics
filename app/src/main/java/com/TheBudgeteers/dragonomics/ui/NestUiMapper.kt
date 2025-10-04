package com.TheBudgeteers.dragonomics.ui

import android.content.Context
import android.graphics.Color
import androidx.annotation.DrawableRes
import com.TheBudgeteers.dragonomics.R
import com.TheBudgeteers.dragonomics.models.Mood

// Maps domain models and raw data to UI representations
// Keeps UI-related transformations out of ViewModels and Adapters
// Used to format values, resolve icons, colors, and map moods to drawables
object NestUiMapper {

    // Returns the drawable resource for a given mood
    @DrawableRes
    fun getMoodDrawable(mood: Mood): Int = when (mood) {
        Mood.POSITIVE -> R.drawable.happy_mood
        Mood.NEUTRAL  -> R.drawable.neutral_mood
        Mood.NEGATIVE -> R.drawable.angry_mood
    }

    // Formats a double as a string
    fun formatCurrency(amount: Double): String = "R${amount.toInt()}"

    // Gets drawable resource ID from an icon name
    // Returns 0 if iconName is null, blank or not found
    @DrawableRes
    fun getIconResource(context: Context, iconName: String?): Int {
        if (iconName.isNullOrBlank()) return 0

        return context.resources.getIdentifier(
            iconName,
            "drawable",
            context.packageName
        )
    }

    // Parses a color string safely
    // Returns a fallback colour if parsing fails
    fun parseColorSafe(colorString: String?, fallback: Int = Color.GRAY): Int {
        if (colorString.isNullOrBlank()) return fallback

        return try {
            Color.parseColor(colorString)
        } catch (e: IllegalArgumentException) {
            fallback
        }
    }
}

