package com.scanni.app.review

import com.scanni.app.processing.PageProcessor

class SaveReviewSessionUseCase(
    private val processor: PageProcessor,
    private val saveReviewedDocument: suspend (String, Long?, List<String>) -> Long
) {
    suspend operator fun invoke(
        title: String,
        folderId: Long?,
        pages: List<ReviewPageState>
    ): Long {
        val processedPages = pages.map { page ->
            processor.process(
                originalPath = page.originalPath,
                mode = page.mode,
                corners = page.corners
            )
        }
        return saveReviewedDocument(title, folderId, processedPages)
    }
}
