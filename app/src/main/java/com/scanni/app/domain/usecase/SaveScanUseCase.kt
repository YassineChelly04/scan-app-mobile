package com.scanni.app.domain.usecase

import com.scanni.app.data.files.PageFileStore
import com.scanni.app.domain.model.CapturedPage
import com.scanni.app.domain.model.PageDraft
import com.scanni.app.domain.ocr.OcrScheduler
import com.scanni.app.domain.processing.PageProcessor
import com.scanni.app.domain.repo.DocumentRepository
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Turns the pages of a scan session into a persisted document: renders the final
 * page images, moves files out of the session cache, writes database rows and
 * queues background OCR.
 */
class SaveScanUseCase(
    private val repository: DocumentRepository,
    private val fileStore: PageFileStore,
    private val processor: PageProcessor,
    private val ocrScheduler: OcrScheduler,
) {
    suspend operator fun invoke(
        title: String,
        folderId: String?,
        pages: List<CapturedPage>,
    ): String = withContext(Dispatchers.Default) {
        require(pages.isNotEmpty()) { "Cannot save an empty scan" }
        val documentId = UUID.randomUUID().toString()
        val drafts = pages.mapIndexed { index, page ->
            val processed = processor.render(
                originalPath = page.originalPath,
                quad = page.quad,
                rotationDeg = page.rotationDeg,
                filter = page.filter,
                maxDimension = PageProcessor.FULL_SIZE,
            )
            val originalFile = fileStore.persistOriginal(documentId, page.id, page.originalPath)
            val processedFile = fileStore.writeProcessed(documentId, page.id, processed)
            val thumbFile = fileStore.writeThumb(documentId, page.id, processed)
            val draft = PageDraft(
                id = page.id,
                position = index,
                originalPath = originalFile.absolutePath,
                processedPath = processedFile.absolutePath,
                thumbPath = thumbFile.absolutePath,
                widthPx = processed.width,
                heightPx = processed.height,
                quad = page.quad,
                rotationDeg = page.rotationDeg,
                filter = page.filter,
            )
            processed.recycle()
            draft
        }
        repository.createDocument(documentId, title, folderId, drafts)
        ocrScheduler.scheduleDocument(documentId)
        documentId
    }
}
