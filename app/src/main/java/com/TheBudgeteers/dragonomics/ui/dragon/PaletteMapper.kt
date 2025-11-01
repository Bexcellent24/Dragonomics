package com.TheBudgeteers.dragonomics.utilities

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.Nullable


/*Purpose:
This util object works by extracting the base of the palettes name from the Id and then based on the id, it will try and find both colors associated
with that id for example Pal_Forest, it will find any color xml values that has forest + body and accessory and will return it. If there isnt anything,
it will return null. Its a clean way of taking one Id and having a "color Scheme" instead of our dragon just being a flat straight color. This is used
in the home activity when assigning the color palettes to the dragon.
*/
data class PaletteColors(
    @ColorRes val bodyColorRes: Int,
    @ColorRes val accessoryColorRes: Int
)


object PaletteMapper {

    private const val PALETTE_PREFIX = "pal_"
    private const val BODY_SUFFIX = "_body"
    private const val ACCESSORY_SUFFIX = "_accessory"


    @Nullable
    fun mapPaletteIdToColors(context: Context, paletteId: String?): PaletteColors? {
        if (paletteId.isNullOrEmpty() || !paletteId.startsWith(PALETTE_PREFIX)) {
            return null
        }

        //  gets the base palette name (e.g., "pal_forest" -> "forest")
        val colorBaseName = paletteId.removePrefix(PALETTE_PREFIX)

        // resolves the two required resource IDs
        val bodyName = colorBaseName + BODY_SUFFIX // e.g., "forest_body"
        val accessoryName = colorBaseName + ACCESSORY_SUFFIX // e.g., "forest_accessory"

        @ColorRes
        val bodyResId = context.resources.getIdentifier(bodyName, "color", context.packageName)
        @ColorRes
        val accessoryResId = context.resources.getIdentifier(accessoryName, "color", context.packageName)

        //  makes sure both resources exist (getIdentifier returns 0 if not found)
        return if (bodyResId != 0 && accessoryResId != 0) {
            PaletteColors(
                bodyColorRes = bodyResId,
                accessoryColorRes = accessoryResId
            )
        } else {

            null
        }
    }
}
// Reference List
//Android Developers, 2025. Data layer. [online] Available at: <https://developer.android.com/topic/architecture/data-layer> [Accessed 3 October 2025].
//Ankiersztajn, M, 2024. Data Mapping In Kotlin Explained. [online] Medium. Available at: <https://proandroiddev.com/data-mapping-in-kotlin-explained-94238b914dac> [Accessed 5 Oct. 2025].
// Lorenzo, M.S. de, 2019. Clean Architecture Guide (with tested examples): Data Flow != Dependency Rule. [online] Medium. Available at: <https://proandroiddev.com/clean-architecture-data-flow-dependency-rule-615ffdd79e29>[Accessed 3 October 2025].



