package com.scanni.app.document

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanni.app.data.repo.DocumentRepository
import com.scanni.app.export.PdfExporter
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

typealias PdfExportAction = (List<String>, File) -> File

class DocumentDetailViewModel(
    private val documentId: Long,
    private val repository: DocumentRepository,
    private val exportPdf: PdfExportAction = PdfExporter()::export,
    private val outputDir: File
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

            val outputFile = File(outputDir, "document-${exportable.id}.pdf")
            val generatedPdf = exportPdf(pagePaths, outputFile)

            _uiState.update { state ->
                state.copy(
                    isExporting = false,
                    generatedPdf = generatedPdf
                )
            }
        }
    }
}
