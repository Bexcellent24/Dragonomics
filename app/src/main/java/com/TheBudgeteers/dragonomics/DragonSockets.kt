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
        hornLeft = AttachmentPoint(x = 115, y = 27, width = 75, height = 75),
        hornRight = AttachmentPoint(x = 157, y = 32, width = 75, height = 75),
        wingLeft = AttachmentPoint(x = -70, y = -3, width = 300, height = 300),
        wingRight = AttachmentPoint(x = 105, y = -10, width = 300, height = 300)
    )

    // sockets for the teen
    val TEEN_DRAGON_SOCKETS = SocketSet(
        hornLeft = AttachmentPoint(x = 125, y = 44, width = 50, height = 50),
        hornRight = AttachmentPoint(x = 153, y = 55, width = 50, height = 50),
        wingLeft = AttachmentPoint(x = -70, y = -3, width = 300, height = 300),
        wingRight = AttachmentPoint(x = 85, y = -8, width = 300, height = 300)
    )

    // sockets for the adult
    val ADULT_DRAGON_SOCKETS = SocketSet(
        hornLeft = AttachmentPoint(x = 118, y = 38, width = 75, height = 75),
        hornRight = AttachmentPoint(x = 131, y = 58, width = 75, height = 75),
        wingLeft = AttachmentPoint(x = -70, y = -3, width = 300, height = 300),
        wingRight = AttachmentPoint(x = 95, y = -8, width = 300, height = 300)
    )
}