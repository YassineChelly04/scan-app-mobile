package com.scanni.app.ocr

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import com.scanni.app.domain.model.OcrResult
import com.scanni.app.domain.model.OcrWord
import com.scanni.app.domain.ocr.OcrEngine
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * On-device Arabic OCR via Tesseract 5 (Tesseract4Android). The `ara`
 * traineddata ships in the APK assets and is copied to app storage on first use,
 * so recognition works fully offline from the first launch.
 */
class TesseractArabicOcrEngine(private val context: Context) : OcrEngine {

    override suspend fun recognize(bitmap: Bitmap): OcrResult = withContext(Dispatchers.Default) {
        val dataParent = ensureTrainedData()
        val api = TessBaseAPI()
        try {
            check(api.init(dataParent.absolutePath, LANGUAGE)) { "Tesseract init failed" }
            api.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)
            api.setImage(bitmap)

            val text = api.utF8Text.orEmpty()
            val words = readWords(api, bitmap.width.toFloat(), bitmap.height.toFloat())
            OcrResult(text, words)
        } finally {
            api.recycle()
        }
    }

    private fun readWords(api: TessBaseAPI, width: Float, height: Float): List<OcrWord> {
        val iterator = api.resultIterator ?: return emptyList()
        val words = ArrayList<OcrWord>()
        try {
            iterator.begin()
            do {
                val level = TessBaseAPI.PageIteratorLevel.RIL_WORD
                val text = iterator.getUTF8Text(level) ?: continue
                if (text.isBlank()) continue
                val rect = iterator.getBoundingRect(level)
                words.add(
                    OcrWord(
                        text = text,
                        left = (rect.left / width).coerceIn(0f, 1f),
                        top = (rect.top / height).coerceIn(0f, 1f),
                        right = (rect.right / width).coerceIn(0f, 1f),
                        bottom = (rect.bottom / height).coerceIn(0f, 1f),
                    ),
                )
            } while (iterator.next(TessBaseAPI.PageIteratorLevel.RIL_WORD))
        } finally {
            iterator.delete()
        }
        return words
    }

    /** Copies assets/tessdata/ara.traineddata to files/tesseract/tessdata once. */
    private fun ensureTrainedData(): File {
        val parent = File(context.filesDir, "tesseract")
        val tessdata = File(parent, "tessdata").apply { mkdirs() }
        val target = File(tessdata, "$LANGUAGE.traineddata")
        val assetPath = "tessdata/$LANGUAGE.traineddata"
        val assetSize = context.assets.openFd(assetPath).use { it.length }
        if (!target.exists() || target.length() != assetSize) {
            context.assets.open(assetPath).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return parent
    }

    private companion object {
        const val LANGUAGE = "ara"
    }
}
