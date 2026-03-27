package com.scanni.app.document

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.scanni.app.data.db.FolderDao
import com.scanni.app.data.db.FolderEntity
import com.scanni.app.data.repo.DocumentRepository
import com.scanni.app.export.PdfExporter
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

typealias PdfExportAction = (List<String>, File) -> File

class DocumentDetailViewModel(
    private val documentId: Long,
    private val repository: DocumentRepository,
    private val observeFolders: () -> Flow<List<FolderEntity>> = { emptyFlow() },
    private val exportPdf: PdfExportAction = PdfExporter()::export,
    private val outputDir: File,
    private val exportDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val _uiState = MutableStateFlow(DocumentDetailUiState())
    val uiState: StateFlow<DocumentDetailUiState> = _uiState.asStateFlow()

    private var currentDocument: ExportableDocument? = null

    init {
        viewModelScope.launch {
            observeFolders().collect { folders ->
                _uiState.update { state ->
                    state.copy(
                        availableFolders = folders.map { folder ->
                            DocumentFolderOption(
                                id = folder.id,
                                name = folder.name
                            )
                        },
                        canSaveMetadata = canSaveMetadata(
                            state = state.copy(
                                availableFolders = folders.map { folder ->
                                    DocumentFolderOption(id = folder.id, name = folder.name)
                                }
                            )
                        )
                    )
                }
            }
        }
    }

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
                    editableTitle = exportable.title,
                    pageCount = exportable.pageCount,
                    ocrStatus = exportable.ocrStatus,
                    selectedFolderId = exportable.folderId,
                    availableFolders = uiState.value.availableFolders,
                    isLoading = false,
                    canShare = exportable.pageImageUris.isNotEmpty(),
                    canSaveMetadata = false
                )
            }
        }
    }

    fun onTitleChange(title: String) {
        _uiState.update { state ->
            val updatedState = state.copy(editableTitle = title)
            updatedState.copy(canSaveMetadata = canSaveMetadata(updatedState))
        }
    }

    fun onFolderSelected(folderId: Long?) {
        _uiState.update { state ->
            val updatedState = state.copy(selectedFolderId = folderId)
            updatedState.copy(canSaveMetadata = canSaveMetadata(updatedState))
        }
    }

    fun onSaveDetailsClick() {
        val document = currentDocument ?: return
        val trimmedTitle = _uiState.value.editableTitle.trim()
        if (trimmedTitle.isBlank()) {
            return
        }

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isSavingMetadata = true,
                    errorMessage = null,
                    canSaveMetadata = false
                )
            }

            try {
                repository.updateDocument(
                    documentId = document.id,
                    title = trimmedTitle,
                    folderId = _uiState.value.selectedFolderId
                )
                currentDocument = document.copy(
                    title = trimmedTitle,
                    folderId = _uiState.value.selectedFolderId
                )
                _uiState.update { state ->
                    val updatedState = state.copy(
                        title = trimmedTitle,
                        editableTitle = trimmedTitle,
                        isSavingMetadata = false
                    )
                    updatedState.copy(canSaveMetadata = canSaveMetadata(updatedState))
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _uiState.update { state ->
                    val updatedState = state.copy(
                        isSavingMetadata = false,
                        errorMessage = error.message ?: "Unable to save details."
                    )
                    updatedState.copy(canSaveMetadata = canSaveMetadata(updatedState))
                }
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

    private fun canSaveMetadata(state: DocumentDetailUiState): Boolean {
        val document = currentDocument ?: return false
        if (state.isLoading || state.isSavingMetadata) {
            return false
        }

        val trimmedTitle = state.editableTitle.trim()
        if (trimmedTitle.isBlank()) {
            return false
        }

        return trimmedTitle != document.title || state.selectedFolderId != document.folderId
    }

    companion object {
        fun factory(
            documentId: Long,
            repository: DocumentRepository,
            folderDao: FolderDao,
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
                            observeFolders = folderDao::observeAll,
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
