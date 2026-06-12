package com.scanni.app.domain.usecase

import com.scanni.app.core.geometry.Quad
import com.scanni.app.data.files.PageFileStore
import com.scanni.app.domain.model.ScanFilter
import com.scanni.app.domain.ocr.OcrScheduler
import com.scanni.app.domain.processing.PageProcessor
import com.scanni.app.domain.repo.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Re-renders a saved page with a new crop/rotation/filter and refreshes its OCR. */
class UpdatePageUseCase(
    private val repository: DocumentRepository,
    private val fileStore: PageFileStore,
    private val processor: PageProcessor,
    private val ocrScheduler: OcrScheduler,
) {
    suspend operator fun invoke(
        pageId: String,
        quad: Quad?,
        rotationDeg: Int,
        filter: ScanFilter,
    ) = withContext(Dispatchers.Default) {
        val page = repository.getPage(pageId) ?: return@withContext
        val processed = processor.render(
            originalPath = page.originalPath,
            quad = quad,
            rotationDeg = rotationDeg,
            filter = filter,
            maxDimension = PageProcessor.FULL_SIZE,
        )
        fileStore.overwriteProcessed(page.processedPath, processed)
        fileStore.overwriteThumb(page.thumbPath, processed)
        repository.updatePageEdit(
            pageId = pageId,
            quad = quad,
            rotationDeg = rotationDeg,
            filter = filter,
            widthPx = processed.width,
            heightPx = processed.height,
        )
        processed.recycle()
        ocrScheduler.scheduleDocument(page.documentId)
    }
}
