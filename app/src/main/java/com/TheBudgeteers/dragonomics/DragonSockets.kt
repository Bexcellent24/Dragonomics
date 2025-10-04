package com.TheBudgeteers.dragonomics


object DragonSockets {

    const val DRAGON_BIG_DP = 450
    const val DRAGON_SMALL_DP = 360

    // was used for scaling, dont need it rn ( will remove it when im done with the teen and adult
    const val DRAGON_REFERENCE_WIDTH_DP = DRAGON_BIG_DP

    // class to hold the attachement points co-ords and size (doesnt really need the size anymore)
    data class AttachmentPoint(val x: Int, val y: Int, val width: Int, val height: Int)
    data class SocketSet(
        val hornLeft: AttachmentPoint,
        val hornRight: AttachmentPoint,
        val wingLeft: AttachmentPoint,
        val wingRight: AttachmentPoint
    )

    data class AccessoryDrawables(val leftResId: Int, val rightResId: Int)

    // sockets for little baby
    val BABY_DRAGON_SOCKETS = SocketSet(
        hornLeft = AttachmentPoint(x = 140, y = 45, width = 75, height = 75),
        hornRight = AttachmentPoint(x = 190, y = 40, width = 75, height = 75),
        wingLeft = AttachmentPoint(x = -35, y = 45, width = 300, height = 300),
        wingRight = AttachmentPoint(x = 135, y = 40, width = 300, height = 300)
    )

    // sockets for the teen
    val TEEN_DRAGON_SOCKETS = SocketSet(
        hornLeft = AttachmentPoint(x = 120, y = 80, width = 70, height = 70),
        hornRight = AttachmentPoint(x = 260, y = 80, width = 70, height = 70),
        wingLeft = AttachmentPoint(x = 5, y = 180, width = 120, height = 120),
        wingRight = AttachmentPoint(x = 325, y = 180, width = 120, height = 120)
    )

    // sockets for the adult
    val ADULT_DRAGON_SOCKETS = SocketSet(
        hornLeft = AttachmentPoint(x = 140, y = 90, width = 80, height = 80),
        hornRight = AttachmentPoint(x = 230, y = 90, width = 80, height = 80),
        wingLeft = AttachmentPoint(x = 0, y = 200, width = 150, height = 150),
        wingRight = AttachmentPoint(x = 300, y = 200, width = 150, height = 150)
    )
}
