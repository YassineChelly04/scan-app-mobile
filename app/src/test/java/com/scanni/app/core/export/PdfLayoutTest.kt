package com.scanni.app.core.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfLayoutTest {

    @Test
    fun `portrait image fits portrait A4 full-bleed`() {
        val box = PdfLayout.pageSizeFor(1500, 2000)
        // Aspect preserved.
        assertEquals(1500f / 2000f, box.widthPt / box.heightPt, 1e-4f)
        assertTrue(box.widthPt <= PdfLayout.A4_SHORT_PT + 0.01f)
        assertTrue(box.heightPt <= PdfLayout.A4_LONG_PT + 0.01f)
        // At least one dimension touches the A4 bound.
        val touchesWidth = Math.abs(box.widthPt - PdfLayout.A4_SHORT_PT) < 0.01f
        val touchesHeight = Math.abs(box.heightPt - PdfLayout.A4_LONG_PT) < 0.01f
        assertTrue(touchesWidth || touchesHeight)
    }

    @Test
    fun `landscape image fits landscape A4`() {
        val box = PdfLayout.pageSizeFor(2000, 1000)
        assertTrue(box.widthPt > box.heightPt)
        assertTrue(box.widthPt <= PdfLayout.A4_LONG_PT + 0.01f)
        assertTrue(box.heightPt <= PdfLayout.A4_SHORT_PT + 0.01f)
    }

    @Test
    fun `word placement converts to bottom-left origin`() {
        val box = PdfLayout.PageBox(widthPt = 500f, heightPt = 1000f)
        val placement = PdfLayout.placeWord(
            page = box,
            left = 0.1f,
            top = 0.1f,
            right = 0.3f,
            bottom = 0.2f,
            measuredWidthAtSize1 = 2f,
        )
        assertNotNull(placement)
        placement!!
        assertEquals(50f, placement.xPt, 1e-3f)

        val heightPt = 0.1f * 1000f
        val expectedFontSize = heightPt * 0.92f
        assertEquals(expectedFontSize, placement.fontSize, 1e-3f)

        val expectedBaseline = 1000f - (0.1f * 1000f + expectedFontSize * PdfLayout.ASCENT_RATIO)
        assertEquals(expectedBaseline, placement.baselineYPt, 1e-3f)

        val expectedScale = (0.2f * 500f) / (2f * expectedFontSize)
        assertEquals(expectedScale, placement.horizontalScale, 1e-3f)
    }

    @Test
    fun `degenerate boxes are rejected`() {
        val box = PdfLayout.PageBox(500f, 1000f)
        assertNull(PdfLayout.placeWord(box, 0.5f, 0.5f, 0.5f, 0.6f, 1f))
        assertNull(PdfLayout.placeWord(box, 0.1f, 0.5f, 0.3f, 0.5f, 1f))
    }

    @Test
    fun `zero measured width falls back to scale 1`() {
        val box = PdfLayout.PageBox(500f, 1000f)
        val placement = PdfLayout.placeWord(box, 0.1f, 0.1f, 0.3f, 0.2f, 0f)
        assertEquals(1f, placement!!.horizontalScale, 1e-5f)
    }

    @Test
    fun `horizontal scale is clamped`() {
        val box = PdfLayout.PageBox(500f, 1000f)
        val wide = PdfLayout.placeWord(box, 0.0f, 0.1f, 1.0f, 0.11f, 0.001f)
        assertEquals(8f, wide!!.horizontalScale, 1e-4f)
    }
}
