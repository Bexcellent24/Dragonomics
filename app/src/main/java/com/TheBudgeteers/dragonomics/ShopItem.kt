package com.TheBudgeteers.dragonomics

// Shop models
enum class ShopTab { PALETTE, HORNS, WINGS }

data class ShopItem(
    val id: String,
    val name: String,
    val price: Int,
    val owned: Boolean = false,
    val equipped: Boolean = false,
    @androidx.annotation.DrawableRes val previewRes: Int = R.drawable.currency
)
