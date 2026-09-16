package com.posepilot.app.utilities

/**
 * Maps un-mirrored analysis-image pixels to view pixels for a preview scaled with FILL_CENTER
 * (PreviewView's default), optionally mirrored for the front camera.
 */
class OverlayTransform(
    imageWidth: Int,
    imageHeight: Int,
    private val viewWidth: Float,
    private val viewHeight: Float,
    private val mirror: Boolean,
    fit: Boolean = false,
) {
    private val scale: Float
    private val offsetX: Float
    private val offsetY: Float

    init {
        val sx = viewWidth / imageWidth.coerceAtLeast(1)
        val sy = viewHeight / imageHeight.coerceAtLeast(1)
        scale = if (fit) minOf(sx, sy) else maxOf(sx, sy)
        offsetX = (viewWidth - imageWidth * scale) / 2f
        offsetY = (viewHeight - imageHeight * scale) / 2f
    }

    fun x(imageX: Double): Float {
        val v = imageX.toFloat() * scale + offsetX
        return if (mirror) viewWidth - v else v
    }

    fun y(imageY: Double): Float = imageY.toFloat() * scale + offsetY

    fun length(imageLength: Double): Float = imageLength.toFloat() * scale
}
