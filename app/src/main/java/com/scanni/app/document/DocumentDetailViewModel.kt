package com.scanni.app.document

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.scanni.app.data.repo.DocumentRepository
import com.scanni.app.export.PdfExporter
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

typealias PdfExportAction = (List<String>, File) -> File

class DocumentDetailViewModel(
    private val documentId: Long,
    private val repository: DocumentRepository,
    private val exportPdf: PdfExportAction = PdfExporter()::export,
    private val outputDir: File,
    private val exportDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val _uiState = MutableStateFlow(DocumentDetailUiState())
    val uiState: StateFlow<DocumentDetailUiState> = _uiState.asStateFlow()

    private var currentDocument: ExportableDocument? = null

    fun load() {
        viewModelScope.launch {
            val exportable = repository.getExportableDocument(documentId)
            currentDocument = exportable
            _uiState.value = if (exportable == null) {
                DocumentDetailUiState(
                    isLoading = false,
                    errorMessage = "Document not found."
                )
            } else {
                DocumentDetailUiState(
                    title = exportable.title,
                    pageCount = exportable.pageCount,
                    ocrStatus = exportable.ocrStatus,
                    isLoading = false,
                    canShare = exportable.pageImageUris.isNotEmpty()
                )
            }
        }
    }

    fun onShareClick() {
        viewModelScope.launch {
            val exportable = currentDocument ?: return@launch
            val pagePaths = exportable.pageImageUris
            if (pagePaths.any { path -> !File(path).exists() }) {
                _uiState.update { state ->
                    state.copy(
                        isExporting = false,
                        errorMessage = "Processed page file missing.",
                        generatedPdf = null
                    )
                }
                return@launch
            }

            _uiState.update { state ->
                state.copy(
                    isExporting = true,
                    errorMessage = null,
                    generatedPdf = null
                )
            }

            try {
                val outputFile = File(outputDir, "document-${exportable.id}.pdf")
                val generatedPdf = withContext(exportDispatcher) {
                    exportPdf(pagePaths, outputFile)
                }

                _uiState.update { state ->
                    state.copy(
                        isExporting = false,
                        generatedPdf = generatedPdf
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _uiState.update { state ->
                    state.copy(
                        isExporting = false,
                        errorMessage = error.message ?: "PDF export failed.",
                        generatedPdf = null
                    )
                }
            }
        }
    }

    fun onGeneratedPdfConsumed() {
        _uiState.update { state -> state.copy(generatedPdf = null) }
    }

    companion object {
        fun factory(
            documentId: Long,
            repository: DocumentRepository,
            pdfExporter: PdfExporter,
            outputDir: File,
            exportDispatcher: CoroutineDispatcher = Dispatchers.IO
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(DocumentDetailViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return DocumentDetailViewModel(
                            documentId = documentId,
                            repository = repository,
                            exportPdf = pdfExporter::export,
                            outputDir = outputDir,
                            exportDispatcher = exportDispatcher
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}
