package com.scanni.app.core.geometry

import kotlin.math.max
import kotlin.math.min

/**
 * Maps between normalized image coordinates and view pixels for an image of
 * [imageWidth] x [imageHeight] displayed inside a [viewWidth] x [viewHeight] view.
 *
 * [ScaleMode.FILL] matches PreviewView's default FILL_CENTER (image center-cropped),
 * [ScaleMode.FIT] matches FIT_CENTER (whole image letterboxed) as used by the crop editor.
 */
data class OverlayTransform(
    val imageWidth: Float,
    val imageHeight: Float,
    val viewWidth: Float,
    val viewHeight: Float,
    val mode: ScaleMode,
) {
    enum class ScaleMode { FILL, FIT }

    val scale: Float = when (mode) {
        ScaleMode.FILL -> max(viewWidth / imageWidth, viewHeight / imageHeight)
        ScaleMode.FIT -> min(viewWidth / imageWidth, viewHeight / imageHeight)
    }

    val offsetX: Float = (viewWidth - imageWidth * scale) / 2f
    val offsetY: Float = (viewHeight - imageHeight * scale) / 2f

    val isValid: Boolean =
        imageWidth > 0 && imageHeight > 0 && viewWidth > 0 && viewHeight > 0

    /** Normalized image point -> view pixels. */
    fun imageToView(point: Vec2): Vec2 = Vec2(
        point.x * imageWidth * scale + offsetX,
        point.y * imageHeight * scale + offsetY,
    )

    /** View pixels -> normalized image point (not clamped). */
    fun viewToImage(point: Vec2): Vec2 = Vec2(
        (point.x - offsetX) / (imageWidth * scale),
        (point.y - offsetY) / (imageHeight * scale),
    )

    fun imageToView(quad: Quad): Quad = Quad(
        imageToView(quad.topLeft),
        imageToView(quad.topRight),
        imageToView(quad.bottomRight),
        imageToView(quad.bottomLeft),
    )
}
