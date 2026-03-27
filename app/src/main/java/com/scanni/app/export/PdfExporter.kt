package com.scanni.app.export

import android.graphics.Color
import android.graphics.pdf.PdfDocument
import java.io.File

class PdfExporter {
    fun export(pagePaths: List<String>, outputFile: File): File {
        val document = PdfDocument()

        try {
            pagePaths.forEachIndexed { index, _ ->
                val pageInfo = PdfDocument.PageInfo.Builder(1240, 1754, index + 1).create()
                val page = document.startPage(pageInfo)
                page.canvas.drawColor(Color.WHITE)
                document.finishPage(page)
            }

            outputFile.parentFile?.mkdirs()
            outputFile.outputStream().use(document::writeTo)
            return outputFile
        } finally {
            document.close()
        }
    }
}
