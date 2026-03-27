package com.scanni.app.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.scanni.app.data.db.AppDatabase
import com.scanni.app.data.db.PageEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalDocumentRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: LocalDocumentRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = LocalDocumentRepository(
            documentDao = db.documentDao(),
            pageTextDao = db.pageTextDao(),
            pageDao = db.pageDao()
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun savePageText_persistsTextAndMakesDocumentSearchable() = runTest {
        val documentId = repository.createDocument(
            title = "Receipt",
            folderId = null,
            pageCount = 1
        )

        repository.savePageText(
            documentId = documentId,
            pageIndex = 0,
            text = "banana bread"
        )

        val pageText = db.pageTextDao().getByDocumentAndPage(documentId, 0)
        val searchResults = repository.observeLibrary("banana").first()

        assertEquals("banana bread", pageText?.text)
        assertEquals(listOf("Receipt"), searchResults.map { it.title })
    }

    @Test
    fun savePageText_overwritesExistingTextForSamePage() = runTest {
        val documentId = repository.createDocument(
            title = "Chemistry Lab",
            folderId = null,
            pageCount = 1
        )

        repository.savePageText(
            documentId = documentId,
            pageIndex = 0,
            text = "draft text"
        )
        repository.savePageText(
            documentId = documentId,
            pageIndex = 0,
            text = "final text"
        )

        val pageText = db.pageTextDao().getByDocumentAndPage(documentId, 0)
        val searchResults = repository.observeLibrary("final").first()

        assertEquals("final text", pageText?.text)
        assertEquals(listOf("Chemistry Lab"), searchResults.map { it.title })
    }

    @Test
    fun getExportableDocument_returnsOrderedProcessedPagePaths() = runTest {
        val documentId = repository.createDocument(
            title = "Physics Chapter 3",
            folderId = null,
            pageCount = 2
        )
        db.pageDao().insertAll(
            listOf(
                PageEntity(
                    documentId = documentId,
                    pageNumber = 2,
                    imageUri = "files/page-2-processed.jpg"
                ),
                PageEntity(
                    documentId = documentId,
                    pageNumber = 1,
                    imageUri = "files/page-1-processed.jpg"
                )
            )
        )

        val exportable = repository.getExportableDocument(documentId)

        assertEquals("Physics Chapter 3", exportable?.title)
        assertEquals(
            listOf("files/page-1-processed.jpg", "files/page-2-processed.jpg"),
            exportable?.pageImageUris
        )
    }

    @Test
    fun getExportableDocument_returnsNullWhenSavedPagesAreIncomplete() = runTest {
        val documentId = repository.createDocument(
            title = "Biology Notes",
            folderId = null,
            pageCount = 2
        )
        db.pageDao().insertAll(
            listOf(
                PageEntity(
                    documentId = documentId,
                    pageNumber = 1,
                    imageUri = "files/page-1-processed.jpg"
                )
            )
        )

        val exportable = repository.getExportableDocument(documentId)

        assertEquals(null, exportable)
    }

    @Test
    fun getExportableDocument_returnsNullWhenAnyProcessedPagePathIsBlank() = runTest {
        val documentId = repository.createDocument(
            title = "History Review",
            folderId = null,
            pageCount = 2
        )
        db.pageDao().insertAll(
            listOf(
                PageEntity(
                    documentId = documentId,
                    pageNumber = 1,
                    imageUri = "files/page-1-processed.jpg"
                ),
                PageEntity(
                    documentId = documentId,
                    pageNumber = 2,
                    imageUri = "   "
                )
            )
        )

        val exportable = repository.getExportableDocument(documentId)

        assertEquals(null, exportable)
    }
}
