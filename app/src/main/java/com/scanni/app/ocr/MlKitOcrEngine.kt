package com.scanni.app.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import kotlinx.coroutines.tasks.await

class MlKitOcrEngine(
    private val appContext: Context,
    private val recognizeText: suspend (String) -> String = { imagePath ->
        val inputImage = InputImage.fromFilePath(
            appContext,
            Uri.fromFile(File(imagePath))
        )
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            .process(inputImage)
            .await()
            .text
    }
) : OcrEngine {
    override suspend fun extractText(imagePath: String): String =
        recognizeText(imagePath).trim()
}
