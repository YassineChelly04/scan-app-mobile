package com.scanni.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.scanni.app.data.db.ScanniDatabase
import com.scanni.app.data.files.PageFileStore
import com.scanni.app.domain.model.OcrStatus
import com.scanni.app.domain.model.PageDraft
import com.scanni.app.domain.model.ScanFilter
import com.scanni.app.domain.model.SearchHit
import com.scanni.app.domain.ocr.OcrScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integration coverage for the data-integrity fixes: writes are atomic, deletes
 * cascade and clear the search index, recognized text is searchable, and OCR
 * results for a deleted page are ignored. Uses an in-memory Room database under
 * Robolectric; a plain Application avoids triggering the real OpenCV load.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class DocumentRepositoryImplTest {

    private lateinit var db: ScanniDatabase
    private lateinit var repository: DocumentRepositoryImpl
    private val cancelledDocs = mutableListOf<String>()

    private val scheduler = object : OcrScheduler {
        override fun scheduleDocument(documentId: String) {}
        override fun cancelDocument(documentId: String) {
            cancelledDocs += documentId
        }
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, ScanniDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DocumentRepositoryImpl(db, PageFileStore(context), scheduler) { FIXED_CLOCK }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `createDocument persists the document with all of its pages`() = runBlocking {
        repository.createDocument("doc", "Receipt", null, listOf(page("p1", 0), page("p2", 1)))

        val document = repository.getDocument("doc")
        assertEquals("Receipt", document?.title)
        assertEquals(2, document?.pageCount)
        assertEquals(2, repository.getPages("doc").size)
    }

    @Test
    fun `deleting a document cancels OCR and cascades its pages`() = runBlocking {
        repository.createDocument("doc", "Doc", null, listOf(page("p1", 0)))

        repository.deleteDocuments(listOf("doc"))

        assertNull(repository.getDocument("doc"))
        assertTrue(repository.getPages("doc").isEmpty())
        assertTrue("OCR worker should be cancelled on delete", cancelledDocs.contains("doc"))
    }

    @Test
    fun `recognized text becomes searchable and is purged when the document is deleted`() =
        runBlocking {
            repository.createDocument("doc", "Invoice", null, listOf(page("p1", 0)))
            repository.setPageOcr("p1", OcrStatus.DONE, "quarterly revenue summary", null)

            assertTrue(search("revenue").any { it.document.id == "doc" })

            repository.deleteDocuments(listOf("doc"))
            assertTrue(search("revenue").isEmpty())
        }

    @Test
    fun `editing a page drops its stale text from search and marks it pending`() = runBlocking {
        repository.createDocument("doc", "Note", null, listOf(page("p1", 0)))
        repository.setPageOcr("p1", OcrStatus.DONE, "obsolete content", null)
        assertFalse(search("obsolete").isEmpty())

        repository.updatePageEdit("p1", null, 90, ScanFilter.GRAYSCALE, 120, 240)

        assertTrue("edited page must leave search until re-OCR", search("obsolete").isEmpty())
        assertEquals(OcrStatus.PENDING, repository.getPage("p1")?.ocrStatus)
    }

    @Test
    fun `OCR results for a missing page are ignored`() = runBlocking {
        // No document/page exists; this must be a no-op, not a stale FTS row.
        repository.setPageOcr("ghost", OcrStatus.DONE, "phantom text", null)
        assertTrue(search("phantom").isEmpty())
    }

    private suspend fun search(query: String): List<SearchHit> =
        withTimeout(SEARCH_TIMEOUT_MS) { repository.search(query).first() }

    private fun page(id: String, position: Int) = PageDraft(
        id = id,
        position = position,
        originalPath = "/data/$id-original.jpg",
        processedPath = "/data/$id-processed.jpg",
        thumbPath = "/data/$id-thumb.jpg",
        widthPx = 1000,
        heightPx = 1400,
        quad = null,
        rotationDeg = 0,
        filter = ScanFilter.AUTO,
    )

    private companion object {
        const val FIXED_CLOCK = 1_000L
        const val SEARCH_TIMEOUT_MS = 5_000L
    }
}
