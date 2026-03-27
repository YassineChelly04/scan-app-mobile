package com.scanni.app.review

import com.scanni.app.data.db.DocumentEntity
import com.scanni.app.data.repo.DocumentRepository
import com.scanni.app.document.ExportableDocument
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SaveReviewedDocumentUseCaseTest {
    @Test
    fun save_persistsDocumentAndEnqueuesOcrForEachPage() = runTest {
        val repository = FakeDocumentRepository()
        val enqueuedJobs = mutableListOf<Triple<String, Long, Int>>()
        val useCase = SaveReviewedDocumentUseCase(
            repository = repository,
            enqueueOcr = { imagePath, documentId, pageIndex ->
                enqueuedJobs += Triple(imagePath, documentId, pageIndex)
            }
        )

        val savedDocumentId = useCase(
            title = "Chemistry Handout",
            folderId = 7L,
            pageImageUris = listOf(
                "files/page-1-processed.jpg",
                "files/page-2-processed.jpg"
            )
        )

        assertEquals(52L, savedDocumentId)
        assertEquals("Chemistry Handout", repository.savedTitle)
        assertEquals(7L, repository.savedFolderId)
        assertEquals(
            listOf("files/page-1-processed.jpg", "files/page-2-processed.jpg"),
            repository.savedPageImageUris
        )
        assertEquals(
            listOf(
                Triple("files/page-1-processed.jpg", 52L, 0),
                Triple("files/page-2-processed.jpg", 52L, 1)
            ),
            enqueuedJobs
        )
    }

    private class FakeDocumentRepository : DocumentRepository {
        var savedTitle: String? = null
        var savedFolderId: Long? = null
        var savedPageImageUris: List<String> = emptyList()

        override fun observeLibrary(query: String): Flow<List<DocumentEntity>> = emptyFlow()

        override suspend fun createDocument(title: String, folderId: Long?, pageCount: Int): Long = 0L

        override suspend fun saveProcessedDocument(
            title: String,
            folderId: Long?,
            pageImageUris: List<String>
        ): Long {
            savedTitle = title
            savedFolderId = folderId
            savedPageImageUris = pageImageUris
            return 52L
        }

        override suspend fun savePageText(documentId: Long, pageIndex: Int, text: String) = Unit

        override suspend fun getExportableDocument(documentId: Long): ExportableDocument? = null
    }
}
