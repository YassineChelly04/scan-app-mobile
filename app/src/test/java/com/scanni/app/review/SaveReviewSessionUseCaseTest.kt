package com.scanni.app.review

import com.scanni.app.processing.EnhancementMode
import com.scanni.app.processing.PageProcessor
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SaveReviewSessionUseCaseTest {
    @Test
    fun save_usesReviewedPageStateForCornersModeRotationAndOrder() = runTest {
        val processedRequests = mutableListOf<ProcessRequest>()
        var savedTitle: String? = null
        var savedFolderId: Long? = null
        var savedPaths: List<String> = emptyList()
        val useCase = SaveReviewSessionUseCase(
            processor = object : PageProcessor {
                override suspend fun process(
                    originalPath: String,
                    mode: EnhancementMode,
                    corners: List<Float>,
                    rotationQuarterTurns: Int
                ): String {
                    processedRequests += ProcessRequest(originalPath, mode, corners, rotationQuarterTurns)
                    return "$originalPath-${mode.name.lowercase()}-$rotationQuarterTurns.jpg"
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
                    originalPath = "files/page-2.jpg",
                    corners = listOf(0.15f, 0.1f, 0.85f, 0.1f, 0.85f, 0.9f, 0.15f, 0.9f),
                    mode = EnhancementMode.WHITEBOARD,
                    rotationQuarterTurns = 3,
                    processedPath = "ignored"
                ),
                ReviewPageState(
                    originalPath = "files/page-1.jpg",
                    corners = listOf(0.05f, 0.05f, 0.95f, 0.05f, 0.95f, 0.95f, 0.05f, 0.95f),
                    mode = EnhancementMode.BOOK,
                    rotationQuarterTurns = 1,
                    processedPath = "ignored"
                )
            )
        )

        assertEquals(81L, result)
        assertEquals("Semester Notes", savedTitle)
        assertEquals(7L, savedFolderId)
        assertEquals(
            listOf(
                "files/page-2.jpg-whiteboard-3.jpg",
                "files/page-1.jpg-book-1.jpg"
            ),
            savedPaths
        )
        assertEquals(
            listOf(
                ProcessRequest(
                    originalPath = "files/page-2.jpg",
                    mode = EnhancementMode.WHITEBOARD,
                    corners = listOf(0.15f, 0.1f, 0.85f, 0.1f, 0.85f, 0.9f, 0.15f, 0.9f),
                    rotationQuarterTurns = 3
                ),
                ProcessRequest(
                    originalPath = "files/page-1.jpg",
                    mode = EnhancementMode.BOOK,
                    corners = listOf(0.05f, 0.05f, 0.95f, 0.05f, 0.95f, 0.95f, 0.05f, 0.95f),
                    rotationQuarterTurns = 1
                )
            ),
            processedRequests
        )
    }

    private data class ProcessRequest(
        val originalPath: String,
        val mode: EnhancementMode,
        val corners: List<Float>,
        val rotationQuarterTurns: Int
    )
}
