package com.scanni.app.export

import android.content.Context
import com.scanni.app.core.export.PdfLayout
import com.scanni.app.domain.model.OcrWords
import com.scanni.app.domain.model.Page
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.state.RenderingMode
import com.tom_roush.pdfbox.util.Matrix
import java.io.File
import java.io.FileInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Writes Lens-style searchable PDFs: each page is the processed JPEG (embedded
 * without recompression) with the recognized words drawn invisibly at their
 * original positions, so text can be selected, copied and searched in any viewer.
 *
 * Latin words use built-in Helvetica; words Helvetica cannot encode (e.g. Arabic)
 * fall back to an embedded Noto Sans Arabic subset.
 */
class SearchablePdfWriter(private val context: Context) {

    suspend fun write(outFile: File, pages: List<Page>) = withContext(Dispatchers.IO) {
        val document = PDDocument()
        var arabicFont: PDFont? = null

        fun fallbackFont(): PDFont? {
            if (arabicFont == null) {
                arabicFont = runCatching {
                    context.assets.open(ARABIC_FONT_ASSET).use { stream ->
                        PDType0Font.load(document, stream, true)
                    }
                }.getOrNull()
            }
            return arabicFont
        }

        try {
            for (page in pages) {
                val imageFile = File(page.processedPath)
                if (!imageFile.exists()) continue
                val box = PdfLayout.pageSizeFor(page.widthPx, page.heightPx)
                val pdPage = PDPage(PDRectangle(box.widthPt, box.heightPt))
                document.addPage(pdPage)

                val image = FileInputStream(imageFile).use { stream ->
                    JPEGFactory.createFromStream(document, stream)
                }

                PDPageContentStream(document, pdPage).use { content ->
                    content.drawImage(image, 0f, 0f, box.widthPt, box.heightPt)

                    val words = OcrWords.decode(page.ocrWordsJson)
                    if (words.isEmpty()) return@use
                    content.setRenderingMode(RenderingMode.NEITHER)
                    for (word in words) {
                        val text = word.text.trim()
                        if (text.isEmpty()) continue

                        var font: PDFont = PDType1Font.HELVETICA
                        var measured = measure(font, text)
                        if (measured == null) {
                            font = fallbackFont() ?: continue
                            measured = measure(font, text) ?: continue
                        }
                        val placement = PdfLayout.placeWord(
                            page = box,
                            left = word.left,
                            top = word.top,
                            right = word.right,
                            bottom = word.bottom,
                            measuredWidthAtSize1 = measured,
                        ) ?: continue

                        runCatching {
                            content.beginText()
                            content.setFont(font, placement.fontSize)
                            content.setTextMatrix(
                                Matrix(
                                    placement.horizontalScale,
                                    0f,
                                    0f,
                                    1f,
                                    placement.xPt,
                                    placement.baselineYPt,
                                ),
                            )
                            content.showText(text)
                            content.endText()
                        }
                    }
                }
            }
            document.save(outFile)
        } finally {
            document.close()
        }
    }

    /** Width of [text] at font size 1, or null when the font cannot encode it. */
    private fun measure(font: PDFont, text: String): Float? =
        runCatching { font.getStringWidth(text) / 1000f }.getOrNull()

    private companion object {
        const val ARABIC_FONT_ASSET = "fonts/NotoSansArabic-Regular.ttf"
    }
}
