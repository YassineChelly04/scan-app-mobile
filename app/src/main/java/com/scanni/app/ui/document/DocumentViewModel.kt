package com.scanni.app.ui.document

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanni.app.domain.model.Document
import com.scanni.app.domain.model.Folder
import com.scanni.app.domain.model.Page
import com.scanni.app.domain.ocr.OcrScheduler
import com.scanni.app.domain.repo.DocumentRepository
import com.scanni.app.domain.usecase.ExportDocumentUseCase
import java.io.File
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DocumentUiState(
    val document: Document? = null,
    val pages: List<Page> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val exporting: Boolean = false,
    val loaded: Boolean = false,
)

sealed interface DocumentEvent {
    data class SharePdf(val file: File) : DocumentEvent
    data class ShareImages(val files: List<File>) : DocumentEvent
    data class CopyText(val text: String) : DocumentEvent
    data object Deleted : DocumentEvent
    data object ExportFailed : DocumentEvent
}

class DocumentViewModel(
    private val documentId: String,
    private val repository: DocumentRepository,
    private val exportDocument: ExportDocumentUseCase,
    private val ocrScheduler: OcrScheduler,
) : ViewModel() {

    private val exporting = MutableStateFlow(false)
    private val loaded = MutableStateFlow(false)

    private val _events = MutableSharedFlow<DocumentEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<DocumentEvent> = _events

    val uiState: StateFlow<DocumentUiState> = combine(
        repository.observeDocument(documentId),
        repository.observePages(documentId),
        repository.observeFolders(),
        exporting,
    ) { document, pages, folders, exporting ->
        loaded.value = true
        DocumentUiState(
            document = document,
            pages = pages,
            folders = folders,
            exporting = exporting,
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DocumentUiState())

    fun sharePdf() = export { _events.tryEmit(DocumentEvent.SharePdf(it)) }

    fun savePdfTo(uri: Uri, copy: suspend (File, Uri) -> Unit) {
        if (exporting.value) return
        exporting.value = true
        viewModelScope.launch {
            runCatching {
                val file = exportDocument.exportPdf(documentId)
                copy(file, uri)
            }.onFailure { _events.tryEmit(DocumentEvent.ExportFailed) }
            exporting.value = false
        }
    }

    fun shareImages() {
        viewModelScope.launch {
            val files = exportDocument.imageFiles(documentId)
            if (files.isEmpty()) {
                _events.tryEmit(DocumentEvent.ExportFailed)
            } else {
                _events.tryEmit(DocumentEvent.ShareImages(files))
            }
        }
    }

    fun copyAllText() {
        viewModelScope.launch {
            val text = exportDocument.allText(documentId)
            _events.tryEmit(DocumentEvent.CopyText(text))
        }
    }

    fun rename(title: String) {
        viewModelScope.launch { repository.renameDocument(documentId, title.trim()) }
    }

    fun moveTo(folderId: String?) {
        viewModelScope.launch { repository.moveDocuments(listOf(documentId), folderId) }
    }

    fun delete() {
        viewModelScope.launch {
            repository.deleteDocuments(listOf(documentId))
            _events.tryEmit(DocumentEvent.Deleted)
        }
    }

    fun rerunOcr() {
        viewModelScope.launch {
            repository.resetOcr(documentId)
            ocrScheduler.scheduleDocument(documentId)
        }
    }

    private fun export(onReady: (File) -> Unit) {
        if (exporting.value) return
        exporting.value = true
        viewModelScope.launch {
            runCatching { exportDocument.exportPdf(documentId) }
                .onSuccess(onReady)
                .onFailure { _events.tryEmit(DocumentEvent.ExportFailed) }
            exporting.value = false
        }
    }
}
