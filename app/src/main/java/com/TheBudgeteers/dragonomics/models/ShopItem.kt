package com.TheBudgeteers.dragonomics.models

import androidx.annotation.DrawableRes
import com.TheBudgeteers.dragonomics.R

// Represents tabs in the shop UI
enum class ShopTab {
    PALETTE, // Colour palettes
    HORNS,   // Horn customisations
    WINGS    // Wing customisations
}

// Model for a single item in the shop
data class ShopItem(
    val id: String,           // Unique ID for item
    val name: String,         // Item name
    val price: Int,           // Cost in currency
    val owned: Boolean = false,   // Whether user owns it
    val equipped: Boolean = false, // Whether user has equipped it
    @DrawableRes val previewRes: Int = R.drawable.currency // Icon for preview
)