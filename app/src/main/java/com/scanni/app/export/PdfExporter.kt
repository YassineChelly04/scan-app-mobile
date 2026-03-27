package com.scanni.app.export

import java.io.File
import java.io.ByteArrayOutputStream
import java.util.Locale

class PdfExporter {
    fun export(pagePaths: List<String>, outputFile: File): File {
        outputFile.parentFile?.mkdirs()
        outputFile.writeBytes(buildPdf(pagePaths.size))
        return outputFile
    }

    private fun buildPdf(pageCount: Int): ByteArray {
        val buffer = ByteArrayOutputStream()
        val objectOffsets = mutableListOf<Int>()
        val pageObjectNumbers = (0 until pageCount).map { 3 + (it * 2) }
        val pageKids = pageObjectNumbers.joinToString(" ") { "$it 0 R" }
        val blankPageStream = "1 1 1 rg\n0 0 1240 1754 re\nf\n"
        val blankPageLength = blankPageStream.toByteArray(Charsets.US_ASCII).size

        fun write(text: String) {
            buffer.write(text.toByteArray(Charsets.US_ASCII))
        }

        fun writeObject(number: Int, body: String) {
            objectOffsets += buffer.size()
            write("$number 0 obj\n")
            write(body)
            if (!body.endsWith("\n")) {
                write("\n")
            }
            write("endobj\n")
        }

        write("%PDF-1.4\n")
        writeObject(1, "<< /Type /Catalog /Pages 2 0 R >>\n")
        writeObject(2, "<< /Type /Pages /Kids [ $pageKids ] /Count $pageCount >>\n")

        pageObjectNumbers.forEach { pageObjectNumber ->
            val contentObjectNumber = pageObjectNumber + 1
            writeObject(
                pageObjectNumber,
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 1240 1754] /Resources << >> /Contents $contentObjectNumber 0 R >>\n"
            )
            writeObject(
                contentObjectNumber,
                "<< /Length $blankPageLength >>\nstream\n$blankPageStream" +
                    "endstream\n"
            )
        }

        val xrefStart = buffer.size()
        write("xref\n0 ${objectOffsets.size + 1}\n")
        write("0000000000 65535 f \n")
        objectOffsets.forEach { offset ->
            write(String.format(Locale.US, "%010d 00000 n \n", offset))
        }
        write("trailer\n<< /Size ${objectOffsets.size + 1} /Root 1 0 R >>\n")
        write("startxref\n$xrefStart\n%%EOF\n")

        return buffer.toByteArray()
    }
}
