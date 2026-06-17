package com.scanni.app.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.scanni.app.domain.model.OcrResult
import com.scanni.app.domain.model.OcrWord
import com.scanni.app.domain.ocr.OcrEngine
import kotlinx.coroutines.tasks.await

/** On-device Latin-script OCR via ML Kit Text Recognition v2. */
class MlKitLatinOcrEngine : OcrEngine {

    // One reusable recognizer for the engine's lifetime — ML Kit clients are
    // thread-safe for process() and expensive to recreate for every page.
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    override suspend fun recognize(bitmap: Bitmap): OcrResult {
        val result = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()
        val words = buildList {
            for (block in result.textBlocks) {
                for (line in block.lines) {
                    for (element in line.elements) {
                        val box = element.boundingBox ?: continue
                        if (element.text.isBlank()) continue
                        add(
                            OcrWord(
                                text = element.text,
                                left = (box.left / width).coerceIn(0f, 1f),
                                top = (box.top / height).coerceIn(0f, 1f),
                                right = (box.right / width).coerceIn(0f, 1f),
                                bottom = (box.bottom / height).coerceIn(0f, 1f),
                            ),
                        )
                    }
                }
            }
        }
        return OcrResult(result.text, words)
    }
}
