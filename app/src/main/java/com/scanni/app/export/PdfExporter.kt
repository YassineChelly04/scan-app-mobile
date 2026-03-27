package com.scanni.app.export

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import java.io.File

class PdfExporter {
    fun export(pagePaths: List<String>, outputFile: File): File {
        val document = PdfDocument()

        try {
            pagePaths.forEachIndexed { index, pagePath ->
                val bitmap = BitmapFactory.decodeFile(pagePath)
                    ?: throw IllegalArgumentException("Unable to decode page image: $pagePath")
                val pageInfo = PdfDocument.PageInfo.Builder(
                    bitmap.width.coerceAtLeast(1),
                    bitmap.height.coerceAtLeast(1),
                    index + 1
                ).create()
                val page = document.startPage(pageInfo)
                try {
                    page.canvas.drawColor(Color.WHITE)
                    page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    document.finishPage(page)
                } finally {
                    bitmap.recycle()
                }
            }

            outputFile.parentFile?.mkdirs()
            outputFile.outputStream().use(document::writeTo)
            return outputFile
        } finally {
            document.close()
        }
    }
}
