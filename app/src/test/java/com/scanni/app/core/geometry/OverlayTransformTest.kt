package com.scanni.app.core.geometry

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayTransformTest {

    @Test
    fun `fit mode letterboxes the image`() {
        val t = OverlayTransform(
            imageWidth = 100f,
            imageHeight = 200f,
            viewWidth = 200f,
            viewHeight = 200f,
            mode = OverlayTransform.ScaleMode.FIT,
        )
        assertEquals(1f, t.scale, 1e-5f)
        assertEquals(50f, t.offsetX, 1e-5f)
        assertEquals(0f, t.offsetY, 1e-5f)

        val center = t.imageToView(Vec2(0.5f, 0.5f))
        assertEquals(100f, center.x, 1e-4f)
        assertEquals(100f, center.y, 1e-4f)
    }

    @Test
    fun `fill mode center-crops the image`() {
        val t = OverlayTransform(
            imageWidth = 100f,
            imageHeight = 200f,
            viewWidth = 200f,
            viewHeight = 200f,
            mode = OverlayTransform.ScaleMode.FILL,
        )
        assertEquals(2f, t.scale, 1e-5f)
        assertEquals(0f, t.offsetX, 1e-5f)
        assertEquals(-100f, t.offsetY, 1e-5f)

        val center = t.imageToView(Vec2(0.5f, 0.5f))
        assertEquals(100f, center.x, 1e-4f)
        assertEquals(100f, center.y, 1e-4f)

        // The top edge of the image lies above the visible view.
        val top = t.imageToView(Vec2(0.5f, 0f))
        assertEquals(-100f, top.y, 1e-4f)
    }

    @Test
    fun `view to image is the inverse of image to view`() {
        val t = OverlayTransform(
            imageWidth = 480f,
            imageHeight = 640f,
            viewWidth = 1080f,
            viewHeight = 1920f,
            mode = OverlayTransform.ScaleMode.FILL,
        )
        val original = Vec2(0.31f, 0.77f)
        val roundTrip = t.viewToImage(t.imageToView(original))
        assertEquals(original.x, roundTrip.x, 1e-4f)
        assertEquals(original.y, roundTrip.y, 1e-4f)
    }

    @Test
    fun `quad mapping maps every corner`() {
        val t = OverlayTransform(
            imageWidth = 100f,
            imageHeight = 100f,
            viewWidth = 200f,
            viewHeight = 200f,
            mode = OverlayTransform.ScaleMode.FIT,
        )
        val mapped = t.imageToView(Quad.FULL)
        assertEquals(0f, mapped.topLeft.x, 1e-4f)
        assertEquals(200f, mapped.bottomRight.x, 1e-4f)
        assertEquals(200f, mapped.bottomRight.y, 1e-4f)
    }
}
