package com.scanni.app.ocr

interface OcrEngine {
    suspend fun extractText(imagePath: String): String
}
