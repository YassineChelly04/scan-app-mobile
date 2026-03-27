package com.scanni.app.review

import androidx.work.WorkManager
import com.scanni.app.data.repo.DocumentRepository
import com.scanni.app.ocr.OcrScheduler

class SaveReviewedDocumentUseCase(
    private val repository: DocumentRepository,
    private val enqueueOcr: (imagePath: String, documentId: Long, pageIndex: Int) -> Unit
) {
    suspend operator fun invoke(
        title: String,
        folderId: Long?,
        pageImageUris: List<String>
    ): Long {
        val documentId = repository.saveProcessedDocument(
            title = title,
            folderId = folderId,
            pageImageUris = pageImageUris
        )
        pageImageUris.forEachIndexed { index, imagePath ->
            enqueueOcr(imagePath, documentId, index)
        }
        return documentId
    }

    companion object {
        fun create(
            repository: DocumentRepository,
            workManager: WorkManager
        ): SaveReviewedDocumentUseCase {
            val scheduler = OcrScheduler(workManager)
            return SaveReviewedDocumentUseCase(repository) { imagePath, documentId, pageIndex ->
                scheduler.enqueue(
                    imagePath = imagePath,
                    documentId = documentId,
                    pageIndex = pageIndex
                )
            }
        }
    }
}
