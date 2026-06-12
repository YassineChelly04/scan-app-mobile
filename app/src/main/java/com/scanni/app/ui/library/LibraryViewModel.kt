package com.scanni.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanni.app.domain.model.Document
import com.scanni.app.domain.model.Folder
import com.scanni.app.domain.model.SearchHit
import com.scanni.app.domain.repo.DocumentRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val query: String = "",
    val folders: List<Folder> = emptyList(),
    val selectedFolderId: String? = null,
    val documents: List<Document> = emptyList(),
    val searchHits: List<SearchHit> = emptyList(),
    val selection: Set<String> = emptySet(),
) {
    val isSearching: Boolean get() = query.isNotBlank()
    val inSelectionMode: Boolean get() = selection.isNotEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class LibraryViewModel(
    private val repository: DocumentRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val selectedFolderId = MutableStateFlow<String?>(null)
    private val selection = MutableStateFlow<Set<String>>(emptySet())

    private val documents = selectedFolderId.flatMapLatest { folderId ->
        repository.observeDocuments(folderId)
    }

    private val searchHits = query
        .debounce(250)
        .flatMapLatest { q ->
            if (q.isBlank()) flowOf(emptyList()) else repository.search(q)
        }

    val uiState: StateFlow<LibraryUiState> = combine(
        query,
        selectedFolderId,
        repository.observeFolders(),
        documents,
        searchHits,
    ) { query, folderId, folders, documents, hits ->
        LibraryUiState(
            query = query,
            folders = folders,
            selectedFolderId = folderId,
            documents = documents,
            searchHits = hits,
        )
    }.combine(selection) { state, selected ->
        state.copy(selection = selected)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun setQuery(value: String) {
        query.value = value
    }

    fun selectFolder(folderId: String?) {
        selectedFolderId.value = folderId
        selection.value = emptySet()
    }

    fun toggleSelection(documentId: String) {
        selection.update { current ->
            if (documentId in current) current - documentId else current + documentId
        }
    }

    fun clearSelection() {
        selection.value = emptySet()
    }

    fun deleteSelected() {
        val ids = selection.value.toList()
        viewModelScope.launch {
            repository.deleteDocuments(ids)
            selection.value = emptySet()
        }
    }

    fun moveSelected(folderId: String?) {
        val ids = selection.value.toList()
        viewModelScope.launch {
            repository.moveDocuments(ids, folderId)
            selection.value = emptySet()
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val id = repository.createFolder(name)
            selectedFolderId.value = id
        }
    }

    fun renameFolder(folderId: String, name: String) {
        viewModelScope.launch { repository.renameFolder(folderId, name) }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            repository.deleteFolder(folderId)
            if (selectedFolderId.value == folderId) selectedFolderId.value = null
        }
    }
}
