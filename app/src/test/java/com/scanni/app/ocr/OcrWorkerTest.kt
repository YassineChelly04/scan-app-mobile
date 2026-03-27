package com.scanni.app.ocr

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.scanni.app.data.db.DocumentEntity
import com.scanni.app.data.repo.DocumentRepository
import com.scanni.app.document.ExportableDocument
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OcrWorkerTest {
    @Test
    fun doWork_withOnlyImagePath_returnsSuccess() = runTest {
        val fakeRepository = FakeDocumentRepository()
        val worker = TestListenableWorkerBuilder<OcrWorker>(
            context = ApplicationProvider.getApplicationContext(),
            inputData = Data.Builder()
                .putString(OcrWorker.KEY_IMAGE_PATH, "files/page-1.jpg")
                .build()
        )
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters
                    ): ListenableWorker = OcrWorker(
                        appContext = appContext,
                        params = workerParameters,
                        ocrEngine = object : OcrEngine {
                            override suspend fun extractText(imagePath: String): String {
                                fakeRepository.extractedPath = imagePath
                                return "recognized text"
                            }
                        },
                        documentRepository = fakeRepository
                    )
                }
            )
            .build()

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals("files/page-1.jpg", fakeRepository.extractedPath)
        assertEquals(null, fakeRepository.savedDocumentId)
        assertEquals(null, fakeRepository.savedPageIndex)
        assertEquals(null, fakeRepository.savedText)
    }

    @Test
    fun doWork_extractsTextPersistsItAndReturnsSuccess() = runTest {
        val fakeRepository = FakeDocumentRepository()
        val worker = TestListenableWorkerBuilder<OcrWorker>(
            context = ApplicationProvider.getApplicationContext(),
            inputData = Data.Builder()
                .putString(OcrWorker.KEY_IMAGE_PATH, "files/page-1.jpg")
                .putLong(OcrWorker.KEY_DOCUMENT_ID, 42L)
                .putInt(OcrWorker.KEY_PAGE_INDEX, 3)
                .build()
        )
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters
                    ): ListenableWorker = OcrWorker(
                        appContext = appContext,
                        params = workerParameters,
                        ocrEngine = object : OcrEngine {
                            override suspend fun extractText(imagePath: String): String {
                                fakeRepository.extractedPath = imagePath
                                return "recognized text"
                            }
                        },
                        documentRepository = fakeRepository
                    )
                }
            )
            .build()

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals("files/page-1.jpg", fakeRepository.extractedPath)
        assertEquals(42L, fakeRepository.savedDocumentId)
        assertEquals(3, fakeRepository.savedPageIndex)
        assertEquals("recognized text", fakeRepository.savedText)
    }

    @Test
    fun doWork_returnsFailureWhenRequiredInputMissing() = runTest {
        val worker = TestListenableWorkerBuilder<OcrWorker>(
            context = ApplicationProvider.getApplicationContext()
        )
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters
                    ): ListenableWorker = OcrWorker(
                        appContext = appContext,
                        params = workerParameters,
                        ocrEngine = object : OcrEngine {
                            override suspend fun extractText(imagePath: String): String =
                                "should not be called"
                        },
                        documentRepository = FakeDocumentRepository()
                    )
                }
            )
            .build()

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
    }

    private class FakeDocumentRepository : DocumentRepository {
        var extractedPath: String? = null
        var savedDocumentId: Long? = null
        var savedPageIndex: Int? = null
        var savedText: String? = null

        override fun observeLibrary(query: String): Flow<List<DocumentEntity>> = emptyFlow()

        override suspend fun createDocument(title: String, folderId: Long?, pageCount: Int): Long = 0L

        override suspend fun saveProcessedDocument(
            title: String,
            folderId: Long?,
            pageImageUris: List<String>
        ): Long = 0L

        override suspend fun savePageText(documentId: Long, pageIndex: Int, text: String) {
            savedDocumentId = documentId
            savedPageIndex = pageIndex
            savedText = text
        }

        override suspend fun getExportableDocument(documentId: Long): ExportableDocument? = null
    }
}
