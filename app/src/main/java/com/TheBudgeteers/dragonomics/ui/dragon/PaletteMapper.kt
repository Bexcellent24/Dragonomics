package com.TheBudgeteers.dragonomics.utilities

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.Nullable

/**
 * LA PURPOSE:
 * Data class holding gradient colors for both body and accessories.
 * Each part has a top and bottom color for  gradients.
 */
data class PaletteColors(
    @ColorRes val bodyTopColorRes: Int,
    @ColorRes val bodyBottomColorRes: Int,
    @ColorRes val accessoryTopColorRes: Int,
    @ColorRes val accessoryBottomColorRes: Int
)

object PaletteMapper {

    private const val PALETTE_PREFIX = "pal_"
    private const val BODY_TOP_SUFFIX = "_body_top"
    private const val BODY_BOTTOM_SUFFIX = "_body_bottom"
    private const val ACCESSORY_TOP_SUFFIX = "_accessory_top"
    private const val ACCESSORY_BOTTOM_SUFFIX = "_accessory_bottom"

    @Nullable
    fun mapPaletteIdToColors(context: Context, paletteId: String?): PaletteColors? {
        if (paletteId.isNullOrEmpty() || !paletteId.startsWith(PALETTE_PREFIX)) {
            return null
        }

        // Extract base palette name (e.g., "pal_forest" -> "forest")
        val colorBaseName = paletteId.removePrefix(PALETTE_PREFIX)

        // Build resource names
        val bodyTopName = colorBaseName + BODY_TOP_SUFFIX
        val bodyBottomName = colorBaseName + BODY_BOTTOM_SUFFIX
        val accessoryTopName = colorBaseName + ACCESSORY_TOP_SUFFIX
        val accessoryBottomName = colorBaseName + ACCESSORY_BOTTOM_SUFFIX

        // Resolve resource IDs
        @ColorRes val bodyTopResId = context.resources.getIdentifier(bodyTopName, "color", context.packageName)
        @ColorRes val bodyBottomResId = context.resources.getIdentifier(bodyBottomName, "color", context.packageName)
        @ColorRes val accessoryTopResId = context.resources.getIdentifier(accessoryTopName, "color", context.packageName)
        @ColorRes val accessoryBottomResId = context.resources.getIdentifier(accessoryBottomName, "color", context.packageName)

        // Ensure all four resources exist
        return if (bodyTopResId != 0 && bodyBottomResId != 0 &&
            accessoryTopResId != 0 && accessoryBottomResId != 0) {
            PaletteColors(
                bodyTopColorRes = bodyTopResId,
                bodyBottomColorRes = bodyBottomResId,
                accessoryTopColorRes = accessoryTopResId,
                accessoryBottomColorRes = accessoryBottomResId
            )
        } else {
            null
        }
    }
}

// References:
// Android Developers, 2025. Data layer. [online] Available at: <https://developer.android.com/topic/architecture/data-layer> [Accessed 3 October 2025].
// Ankiersztajn, M, 2024. Data Mapping In Kotlin Explained. [online] Medium. Available at: <https://proandroiddev.com/data-mapping-in-kotlin-explained-94238b914dac> [Accessed 5 Oct. 2025].