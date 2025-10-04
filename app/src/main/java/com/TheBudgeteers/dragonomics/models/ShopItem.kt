package com.TheBudgeteers.dragonomics.models

import androidx.annotation.DrawableRes
import com.TheBudgeteers.dragonomics.R

// Shop models
enum class ShopTab { PALETTE, HORNS, WINGS }

data class ShopItem(
    val id: String,
    val name: String,
    val price: Int,
    val owned: Boolean = false,
    val equipped: Boolean = false,
    @DrawableRes val previewRes: Int = R.drawable.currency
)