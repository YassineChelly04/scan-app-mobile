package com.scanni.app.ocr

import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class OcrScheduler(
    private val workManager: WorkManager
) {
    fun enqueue(imagePath: String, documentId: Long? = null, pageIndex: Int? = null) {
        val inputData = Data.Builder()
            .putString(OcrWorker.KEY_IMAGE_PATH, imagePath)
            .apply {
                if (documentId != null) {
                    putLong(OcrWorker.KEY_DOCUMENT_ID, documentId)
                }
                if (pageIndex != null) {
                    putInt(OcrWorker.KEY_PAGE_INDEX, pageIndex)
                }
            }
            .build()
        val request = OneTimeWorkRequestBuilder<OcrWorker>()
            .setInputData(inputData)
            .build()
        workManager.enqueue(request)
    }
}
