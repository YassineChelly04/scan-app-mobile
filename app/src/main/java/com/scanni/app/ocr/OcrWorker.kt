package com.scanni.app.ocr

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.scanni.app.data.db.AppDatabase
import com.scanni.app.data.repo.DocumentRepository
import com.scanni.app.data.repo.LocalDocumentRepository

class OcrWorker @JvmOverloads constructor(
    appContext: Context,
    params: WorkerParameters,
    private val ocrEngine: OcrEngine = MlKitOcrEngine(),
    private val documentRepository: DocumentRepository = defaultRepository(appContext)
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val imagePath = inputData.getString(KEY_IMAGE_PATH)
            ?.takeIf { it.isNotBlank() }
            ?: return Result.failure()
        val documentId = inputData.getLong(KEY_DOCUMENT_ID, INVALID_DOCUMENT_ID)
            .takeIf { it != INVALID_DOCUMENT_ID }
        val pageIndex = inputData.getInt(KEY_PAGE_INDEX, INVALID_PAGE_INDEX)
            .takeIf { it != INVALID_PAGE_INDEX }

        if ((documentId == null) != (pageIndex == null)) {
            return Result.failure()
        }

        val extractedText = ocrEngine.extractText(imagePath)
        if (documentId != null && pageIndex != null) {
            documentRepository.savePageText(documentId, pageIndex, extractedText)
        }
        return Result.success()
    }

    companion object {
        private const val INVALID_DOCUMENT_ID = -1L
        private const val INVALID_PAGE_INDEX = -1

        const val KEY_DOCUMENT_ID = "documentId"
        const val KEY_PAGE_INDEX = "pageIndex"
        const val KEY_IMAGE_PATH = "imagePath"

        private fun defaultRepository(context: Context): DocumentRepository {
            val database = AppDatabase.getInstance(context)
            return LocalDocumentRepository(
                documentDao = database.documentDao(),
                pageTextDao = database.pageTextDao(),
                pageDao = database.pageDao()
            )
        }
    }
}
