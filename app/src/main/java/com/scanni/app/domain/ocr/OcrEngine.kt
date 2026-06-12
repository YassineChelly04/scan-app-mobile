package com.scanni.app.domain.ocr

import android.graphics.Bitmap
import com.scanni.app.domain.model.OcrResult

/** On-device text recognition for one page image. */
interface OcrEngine {
    suspend fun recognize(bitmap: Bitmap): OcrResult
}
