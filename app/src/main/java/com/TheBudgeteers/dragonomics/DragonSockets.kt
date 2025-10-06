package com.TheBudgeteers.dragonomics


object DragonSockets {
    /*
    Purpose:
    This script here holds the attachment points in which our accessories such as our horns and wings will attach to the dragons body.
     Due to our dragon evolving, each form has a different attachement points for these accessories.
     It will communicate with our HomeActivty to place down the accessories in its respective places.
    */

    const val DRAGON_SMALL_DP = 360

    // Used for scaling
    const val DRAGON_REFERENCE_WIDTH_DP = DRAGON_SMALL_DP

    const val DRAGON_VIEW_PADDING_DP = 70

    // class to hold the attachement points co-ords and size
    data class AttachmentPoint(val x: Int, val y: Int, val width: Int, val height: Int)
    data class SocketSet(
        val hornLeft: AttachmentPoint,
        val hornRight: AttachmentPoint,
        val wingLeft: AttachmentPoint,
        val wingRight: AttachmentPoint
    )
    // class to  hold what is a left-handside accessory and whats right hand side
    data class AccessoryDrawables(val leftResId: Int, val rightResId: Int)

    // sockets for little baby
    val BABY_DRAGON_SOCKETS = SocketSet(

        hornLeft = AttachmentPoint(x = 45, y = -43, width = 75, height = 75),

        hornRight = AttachmentPoint(x = 87, y = -38, width = 75, height = 75),

        wingLeft = AttachmentPoint(x = -140, y = -73, width = 300, height = 300),

        wingRight = AttachmentPoint(x = 35, y = -80, width = 300, height = 300)
    )

    // sockets for teen
    val TEEN_DRAGON_SOCKETS = SocketSet(

        hornLeft = AttachmentPoint(x = 55, y = -26, width = 50, height = 50),

        hornRight = AttachmentPoint(x = 83, y = -15, width = 50, height = 50),

        wingLeft = AttachmentPoint(x = -140, y = -73, width = 300, height = 300),

        wingRight = AttachmentPoint(x = 15, y = -78, width = 300, height = 300)
    )

    // sockets for adult
    val ADULT_DRAGON_SOCKETS = SocketSet(

        hornLeft = AttachmentPoint(x = 48, y = -32, width = 75, height = 75),

        hornRight = AttachmentPoint(x = 61, y = -12, width = 75, height = 75),

        wingLeft = AttachmentPoint(x = -140, y = -73, width = 300, height = 300),

        wingRight = AttachmentPoint(x = 25, y = -78, width = 300, height = 300)
    )
}