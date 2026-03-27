package com.scanni.app.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.OutputStream
import java.util.WeakHashMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowPdfDocument::class, ShadowPdfDocumentPage::class])
class PdfExporterTest {
    @Test
    fun export_drawsSourcePageContentOntoPdfPage() {
        val sourceImage = createSolidColorImage(
            fileName = "source-page.png",
            color = Color.rgb(24, 128, 72)
        )

        ShadowPdfDocumentPage.reset()

        PdfExporter().export(
            pagePaths = listOf(sourceImage.absolutePath),
            outputFile = File("build/test-output/rendered.pdf")
        )

        val firstPage = ShadowPdfDocumentPage.firstPageBitmap()
        assertEquals(
            Color.rgb(24, 128, 72),
            firstPage.getPixel(firstPage.width / 2, firstPage.height / 2)
        )
    }

    @Test
    fun export_writesPdfFileToDisk() {
        val samplePagePath = createSolidColorImage(
            fileName = "document-page.png",
            color = Color.rgb(48, 96, 180)
        ).absolutePath
        val output = File("build/test-output/notes.pdf")
        val exporter = PdfExporter()

        assertTrue(File(samplePagePath).exists())

        exporter.export(
            pagePaths = listOf(samplePagePath),
            outputFile = output
        )

        assertTrue(output.exists())
        assertTrue(output.length() > 0)
    }

    private fun createSolidColorImage(fileName: String, color: Int): File {
        val file = File("build/test-output/$fileName").apply {
            parentFile?.mkdirs()
        }
        Bitmap.createBitmap(40, 60, Bitmap.Config.ARGB_8888).apply {
            eraseColor(color)
            file.outputStream().use { stream ->
                compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            recycle()
        }
        return file
    }
}

@Implements(PdfDocument::class)
class ShadowPdfDocument {
    private val pages = mutableListOf<PdfDocument.Page>()
    private var closed = false

    @Implementation
    fun __constructor__() = Unit

    @Implementation
    fun startPage(pageInfo: PdfDocument.PageInfo): PdfDocument.Page {
        check(!closed) { "document is closed!" }
        val page = ReflectionHelpers.callConstructor(PdfDocument.Page::class.java)
        ShadowPdfDocumentPage.register(page, pageInfo)
        return page
    }

    @Implementation
    fun finishPage(page: PdfDocument.Page) {
        check(!closed) { "document is closed!" }
        pages += page
    }

    @Implementation
    fun writeTo(outputStream: OutputStream) {
        check(!closed) { "document is closed!" }
        outputStream.write("%PDF-shadow ${pages.size}".toByteArray(Charsets.US_ASCII))
    }

    @Implementation
    fun close() {
        closed = true
    }
}

@Implements(PdfDocument.Page::class)
class ShadowPdfDocumentPage {
    @Implementation
    fun getCanvas(): Canvas = stateFor(realPage).canvas

    @Implementation
    fun getInfo(): PdfDocument.PageInfo = stateFor(realPage).pageInfo

    companion object {
        private val pageStates = WeakHashMap<PdfDocument.Page, PageState>()

        fun register(page: PdfDocument.Page, pageInfo: PdfDocument.PageInfo) {
            val width = pageInfo.pageWidth.coerceAtLeast(1)
            val height = pageInfo.pageHeight.coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            pageStates[page] = PageState(
                canvas = Canvas(bitmap),
                pageInfo = pageInfo,
                bitmap = bitmap
            )
        }

        fun firstPageBitmap(): Bitmap = pageStates.values.firstOrNull()?.bitmap
            ?: error("No rendered pages captured.")

        fun reset() {
            pageStates.clear()
        }

        private fun stateFor(page: PdfDocument.Page): PageState =
            pageStates[page] ?: error("No shadow state registered for page.")
    }

    private data class PageState(
        val canvas: Canvas,
        val pageInfo: PdfDocument.PageInfo,
        val bitmap: Bitmap
    )

    @org.robolectric.annotation.RealObject
    private lateinit var realPage: PdfDocument.Page
}
