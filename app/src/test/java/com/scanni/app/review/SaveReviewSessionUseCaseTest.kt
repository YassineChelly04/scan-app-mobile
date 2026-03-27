package com.scanni.app.review

import com.scanni.app.camera.CapturedPageDraft
import com.scanni.app.processing.EnhancementMode
import com.scanni.app.processing.PageProcessor
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SaveReviewSessionUseCaseTest {
    @Test
    fun save_processesEachCapturedPageAndPersistsOrderedResults() = runTest {
        val processedRequests = mutableListOf<Triple<String, EnhancementMode, List<Float>>>()
        var savedTitle: String? = null
        var savedFolderId: Long? = null
        var savedPaths: List<String> = emptyList()
        val useCase = SaveReviewSessionUseCase(
            processor = object : PageProcessor {
                override suspend fun process(
                    originalPath: String,
                    mode: EnhancementMode,
                    corners: List<Float>
                ): String {
                    processedRequests += Triple(originalPath, mode, corners)
                    return "$originalPath-${mode.name.lowercase()}.jpg"
                }
            },
            saveReviewedDocument = { title, folderId, pageImageUris ->
                savedTitle = title
                savedFolderId = folderId
                savedPaths = pageImageUris
                81L
            }
        )

        val result = useCase(
            title = "Semester Notes",
            folderId = 7L,
            pages = listOf(
                CapturedPageDraft(
                    originalPath = "files/page-1.jpg",
                    previewPath = "files/page-1-preview.jpg",
                    detectedCorners = listOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)
                ),
                CapturedPageDraft(
                    originalPath = "files/page-2.jpg",
                    previewPath = "files/page-2-preview.jpg",
                    detectedCorners = listOf(10f, 10f, 11f, 10f, 11f, 11f, 10f, 11f)
                )
            ),
            mode = EnhancementMode.BOOK
        )

        assertEquals(81L, result)
        assertEquals("Semester Notes", savedTitle)
        assertEquals(7L, savedFolderId)
        assertEquals(
            listOf(
                "files/page-1.jpg-book.jpg",
                "files/page-2.jpg-book.jpg"
            ),
            savedPaths
        )
        assertEquals(
            listOf(
                Triple("files/page-1.jpg", EnhancementMode.BOOK, listOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)),
                Triple("files/page-2.jpg", EnhancementMode.BOOK, listOf(10f, 10f, 11f, 10f, 11f, 11f, 10f, 11f))
            ),
            processedRequests
        )
    }
}
