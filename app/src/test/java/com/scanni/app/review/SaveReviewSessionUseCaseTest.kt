package com.scanni.app.review

import com.scanni.app.processing.EnhancementMode
import com.scanni.app.processing.PageProcessor
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SaveReviewSessionUseCaseTest {
    @Test
    fun save_usesReviewedPageStateForCornersAndMode() = runTest {
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
                ReviewPageState(
                    originalPath = "files/page-1.jpg",
                    corners = listOf(0.05f, 0.05f, 0.95f, 0.05f, 0.95f, 0.95f, 0.05f, 0.95f),
                    mode = EnhancementMode.BOOK,
                    processedPath = "ignored"
                ),
                ReviewPageState(
                    originalPath = "files/page-2.jpg",
                    corners = listOf(0.15f, 0.1f, 0.85f, 0.1f, 0.85f, 0.9f, 0.15f, 0.9f),
                    mode = EnhancementMode.WHITEBOARD,
                    processedPath = "ignored"
                )
            )
        )

        assertEquals(81L, result)
        assertEquals("Semester Notes", savedTitle)
        assertEquals(7L, savedFolderId)
        assertEquals(
            listOf(
                "files/page-1.jpg-book.jpg",
                "files/page-2.jpg-whiteboard.jpg"
            ),
            savedPaths
        )
        assertEquals(
            listOf(
                Triple(
                    "files/page-1.jpg",
                    EnhancementMode.BOOK,
                    listOf(0.05f, 0.05f, 0.95f, 0.05f, 0.95f, 0.95f, 0.05f, 0.95f)
                ),
                Triple(
                    "files/page-2.jpg",
                    EnhancementMode.WHITEBOARD,
                    listOf(0.15f, 0.1f, 0.85f, 0.1f, 0.85f, 0.9f, 0.15f, 0.9f)
                )
            ),
            processedRequests
        )
    }
}
