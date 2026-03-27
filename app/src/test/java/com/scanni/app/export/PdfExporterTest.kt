package com.scanni.app.export

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PdfExporterTest {
    @Test
    fun export_writesPdfFileToDisk() {
        val output = File("build/test-output/notes.pdf")
        val exporter = PdfExporter()

        exporter.export(
            pagePaths = listOf("ignored-page-path"),
            outputFile = output
        )

        assertTrue(output.exists())
        assertTrue(output.length() > 0)
    }
}
