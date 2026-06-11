package com.scanni.app.core.export

import kotlin.math.min

/**
 * Pure geometry for searchable PDF export: page sizing and the placement of
 * invisible OCR words over the page image. PDF coordinates have their origin
 * at the bottom-left corner of the page.
 */
object PdfLayout {

    const val A4_SHORT_PT = 595.28f
    const val A4_LONG_PT = 841.89f

    /** Fraction of the font size that sits above the baseline, approximated for Latin/Arabic. */
    const val ASCENT_RATIO = 0.76f

    data class PageBox(val widthPt: Float, val heightPt: Float)

    data class WordPlacement(
        val xPt: Float,
        val baselineYPt: Float,
        val fontSize: Float,
        /** Multiplier stretching the rendered string to the OCR box width. */
        val horizontalScale: Float,
    )

    /**
     * Full-bleed page exactly matching the image aspect ratio, fitted inside an
     * A4 sheet whose orientation follows the image.
     */
    fun pageSizeFor(imageWidthPx: Int, imageHeightPx: Int): PageBox {
        require(imageWidthPx > 0 && imageHeightPx > 0) { "Image size must be positive" }
        val landscape = imageWidthPx >= imageHeightPx
        val boundsW = if (landscape) A4_LONG_PT else A4_SHORT_PT
        val boundsH = if (landscape) A4_SHORT_PT else A4_LONG_PT
        val scale = min(boundsW / imageWidthPx, boundsH / imageHeightPx)
        return PageBox(imageWidthPx * scale, imageHeightPx * scale)
    }

    /**
     * Places one OCR word. The box (`left/top/right/bottom`) is normalized to the
     * page image, y pointing down. [measuredWidthAtSize1] is the width of the word
     * rendered at font size 1 (PDFBox: `font.getStringWidth(text) / 1000f`).
     */
    fun placeWord(
        page: PageBox,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        measuredWidthAtSize1: Float,
    ): WordPlacement? {
        val widthPt = (right - left) * page.widthPt
        val heightPt = (bottom - top) * page.heightPt
        if (widthPt <= 0f || heightPt <= 0f) return null

        val fontSize = heightPt * 0.92f
        val baselineY = page.heightPt - (top * page.heightPt + fontSize * ASCENT_RATIO)

        val naturalWidth = measuredWidthAtSize1 * fontSize
        val horizontalScale = if (naturalWidth > 0f) {
            (widthPt / naturalWidth).coerceIn(0.2f, 8f)
        } else {
            1f
        }
        return WordPlacement(
            xPt = left * page.widthPt,
            baselineYPt = baselineY,
            fontSize = fontSize,
            horizontalScale = horizontalScale,
        )
    }
}
