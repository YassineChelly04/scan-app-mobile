package com.scanni.app.review

import com.scanni.app.camera.CapturedPageDraft
import com.scanni.app.processing.EnhancementMode
import com.scanni.app.processing.PageProcessor

class SaveReviewSessionUseCase(
    private val processor: PageProcessor,
    private val saveReviewedDocument: suspend (String, Long?, List<String>) -> Long
) {
    suspend operator fun invoke(
        title: String,
        folderId: Long?,
        pages: List<CapturedPageDraft>,
        mode: EnhancementMode
    ): Long {
        val processedPages = pages.map { page ->
            processor.process(
                originalPath = page.originalPath,
                mode = mode,
                corners = page.detectedCorners
            )
        }
        return saveReviewedDocument(title, folderId, processedPages)
    }
}
