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
        hornLeft = AttachmentPoint(x = 140, y = 52, width = 75, height = 75),
        hornRight = AttachmentPoint(x = 190, y = 52, width = 75, height = 75),
        wingLeft = AttachmentPoint(x = -35, y = 55, width = 300, height = 300),
        wingRight = AttachmentPoint(x = 135, y = 50, width = 300, height = 300)
    )

    // sockets for the teen
    val TEEN_DRAGON_SOCKETS = SocketSet(
        hornLeft = AttachmentPoint(x = 160, y = 64, width = 50, height = 50),
        hornRight = AttachmentPoint(x = 193, y = 75, width = 50, height = 50),
        wingLeft = AttachmentPoint(x = -35, y = 55, width = 300, height = 300),
        wingRight = AttachmentPoint(x = 135, y = 50, width = 300, height = 300)
    )

    // sockets for the adult
    val ADULT_DRAGON_SOCKETS = SocketSet(
        hornLeft = AttachmentPoint(x = 152, y = 62, width = 75, height = 75),
        hornRight = AttachmentPoint(x = 168, y = 80, width = 75, height = 75),
        wingLeft = AttachmentPoint(x = -35, y = 50, width = 300, height = 300),
        wingRight = AttachmentPoint(x = 140, y = 50, width = 300, height = 300)
    )
}
