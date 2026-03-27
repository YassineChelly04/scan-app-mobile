package com.scanni.app.document

import com.scanni.app.data.db.DocumentEntity
import com.scanni.app.data.repo.DocumentRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val outputDir = File("build/test-output/document-detail")

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        outputDir.mkdirs()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        outputDir.deleteRecursively()
    }

    @Test
    fun load_populatesDetailState() = runTest {
        val viewModel = DocumentDetailViewModel(
            documentId = 7L,
            repository = FakeDocumentRepository(
                exportableDocument = ExportableDocument(
                    id = 7L,
                    title = "Chemistry Notes",
                    pageCount = 2,
                    ocrStatus = "complete",
                    pageImageUris = listOf(
                        File(outputDir, "page-1.jpg").absolutePath,
                        File(outputDir, "page-2.jpg").absolutePath
                    )
                )
            ),
            exportPdf = { _, outputFile -> outputFile },
            outputDir = outputDir
        )

        viewModel.load()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Chemistry Notes", state.title)
        assertEquals(2, state.pageCount)
        assertEquals("complete", state.ocrStatus)
        assertFalse(state.isLoading)
        assertTrue(state.canShare)
        assertNull(state.errorMessage)
        assertNull(state.generatedPdf)
    }

    @Test
    fun load_withMissingDocument_setsNonShareableState() = runTest {
        val viewModel = DocumentDetailViewModel(
            documentId = 99L,
            repository = FakeDocumentRepository(exportableDocument = null),
            exportPdf = { _, outputFile -> outputFile },
            outputDir = outputDir
        )

        viewModel.load()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isExporting)
        assertFalse(state.canShare)
        assertEquals("Document not found.", state.errorMessage)
        assertNull(state.generatedPdf)
    }

    @Test
    fun onShareClick_withMissingProcessedPage_setsError() = runTest {
        val missingPath = File(outputDir, "missing.jpg").absolutePath
        val viewModel = DocumentDetailViewModel(
            documentId = 9L,
            repository = FakeDocumentRepository(
                exportableDocument = ExportableDocument(
                    id = 9L,
                    title = "Biology Notes",
                    pageCount = 1,
                    ocrStatus = "complete",
                    pageImageUris = listOf(missingPath)
                )
            ),
            exportPdf = { _, outputFile -> outputFile },
            outputDir = outputDir
        )

        viewModel.load()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onShareClick()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Processed page file missing.", state.errorMessage)
        assertNull(state.generatedPdf)
        assertFalse(state.isExporting)
    }

    @Test
    fun onShareClick_whenExporterFails_setsErrorAndClearsExportingState() = runTest {
        val pageOne = File(outputDir, "page-1.jpg").apply {
            parentFile?.mkdirs()
            writeText("page-1")
        }
        val viewModel = DocumentDetailViewModel(
            documentId = 15L,
            repository = FakeDocumentRepository(
                exportableDocument = ExportableDocument(
                    id = 15L,
                    title = "Calculus Notes",
                    pageCount = 1,
                    ocrStatus = "complete",
                    pageImageUris = listOf(pageOne.absolutePath)
                )
            ),
            exportPdf = { _, _ -> throw IllegalStateException("Export failed.") },
            outputDir = outputDir,
            exportDispatcher = dispatcher
        )

        viewModel.load()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onShareClick()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Export failed.", state.errorMessage)
        assertFalse(state.isExporting)
        assertNull(state.generatedPdf)
    }

    @Test
    fun onShareClick_withAvailableProcessedPages_generatesPdf() = runTest {
        val pageOne = File(outputDir, "page-1.jpg").apply {
            parentFile?.mkdirs()
            writeText("page-1")
        }
        val pageTwo = File(outputDir, "page-2.jpg").apply {
            writeText("page-2")
        }
        var exportedPaths: List<String>? = null
        var exportedOutputFile: File? = null
        val generatedFile = File(outputDir, "document-12.pdf")
        val viewModel = DocumentDetailViewModel(
            documentId = 12L,
            repository = FakeDocumentRepository(
                exportableDocument = ExportableDocument(
                    id = 12L,
                    title = "Physics Notes",
                    pageCount = 2,
                    ocrStatus = "complete",
                    pageImageUris = listOf(pageOne.absolutePath, pageTwo.absolutePath)
                )
            ),
            exportPdf = { pagePaths, outputFile ->
                exportedPaths = pagePaths
                exportedOutputFile = outputFile
                generatedFile
            },
            outputDir = outputDir,
            exportDispatcher = dispatcher
        )

        viewModel.load()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onShareClick()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(pageOne.absolutePath, pageTwo.absolutePath), exportedPaths)
        assertEquals(File(outputDir, "document-12.pdf"), exportedOutputFile)
        assertEquals(generatedFile, state.generatedPdf)
        assertNull(state.errorMessage)
        assertFalse(state.isExporting)
        assertTrue(state.canShare)
        assertNotNull(state.generatedPdf)
    }

    @Test
    fun onGeneratedPdfConsumed_clearsGeneratedPdf() = runTest {
        val pageOne = File(outputDir, "page-1.jpg").apply {
            parentFile?.mkdirs()
            writeText("page-1")
        }
        val generatedFile = File(outputDir, "document-30.pdf")
        val viewModel = DocumentDetailViewModel(
            documentId = 30L,
            repository = FakeDocumentRepository(
                exportableDocument = ExportableDocument(
                    id = 30L,
                    title = "Literature Notes",
                    pageCount = 1,
                    ocrStatus = "complete",
                    pageImageUris = listOf(pageOne.absolutePath)
                )
            ),
            exportPdf = { _, _ -> generatedFile },
            outputDir = outputDir,
            exportDispatcher = dispatcher
        )

        viewModel.load()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onShareClick()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(generatedFile, viewModel.uiState.value.generatedPdf)

        viewModel.onGeneratedPdfConsumed()

        assertNull(viewModel.uiState.value.generatedPdf)
    }

    private class FakeDocumentRepository(
        private val exportableDocument: ExportableDocument?
    ) : DocumentRepository {
        override fun observeLibrary(query: String): Flow<List<DocumentEntity>> = emptyFlow()

        override suspend fun createDocument(title: String, folderId: Long?, pageCount: Int): Long = 0L

        override suspend fun savePageText(documentId: Long, pageIndex: Int, text: String) = Unit

        override suspend fun getExportableDocument(documentId: Long): ExportableDocument? = exportableDocument
    }
}
