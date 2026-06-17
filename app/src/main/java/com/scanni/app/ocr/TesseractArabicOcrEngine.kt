package com.scanni.app.ocr

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import com.scanni.app.domain.model.OcrResult
import com.scanni.app.domain.model.OcrWord
import com.scanni.app.domain.ocr.OcrEngine
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * On-device Arabic OCR via Tesseract 5 (Tesseract4Android). The `ara`
 * traineddata ships in the APK assets and is copied to app storage on first use,
 * so recognition works fully offline from the first launch.
 *
 * This single engine instance can be hit by several OCR workers at once (one per
 * document), so recognition is serialized with a [Mutex] and the traineddata is
 * materialized atomically — concurrent callers must never race on the native
 * init or on writing the model file (which previously risked a native crash).
 */
class TesseractArabicOcrEngine(private val context: Context) : OcrEngine {

    private val mutex = Mutex()

    override suspend fun recognize(bitmap: Bitmap): OcrResult = withContext(Dispatchers.Default) {
        mutex.withLock {
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

    /**
     * Copies assets/tessdata/ara.traineddata to files/tesseract/tessdata once.
     * The copy is staged to a temp file and atomically renamed, so a concurrent
     * reader (or a crash mid-copy) can never observe a half-written model.
     */
    private fun ensureTrainedData(): File {
        val parent = File(context.filesDir, "tesseract")
        val tessdata = File(parent, "tessdata").apply { mkdirs() }
        val target = File(tessdata, "$LANGUAGE.traineddata")
        val assetPath = "tessdata/$LANGUAGE.traineddata"
        val assetSize = context.assets.openFd(assetPath).use { it.length }
        if (target.exists() && target.length() == assetSize) return parent

        val tmp = File(tessdata, "$LANGUAGE.traineddata.tmp")
        context.assets.open(assetPath).use { input ->
            tmp.outputStream().use { output -> input.copyTo(output) }
        }
        if (!tmp.renameTo(target)) {
            tmp.copyTo(target, overwrite = true)
            tmp.delete()
        }
        return parent
    }

    private companion object {
        const val LANGUAGE = "ara"
    }
}
