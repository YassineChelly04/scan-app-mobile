package com.scanni.app.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.scanni.app.data.db.FolderDao
import com.scanni.app.data.repo.DocumentRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val repository: DocumentRepository,
    private val folderDao: FolderDao
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val selectedFolderId = MutableStateFlow<Long?>(null)

    private val folders = folderDao.observeAll()
        .map { folderEntities ->
            folderEntities.map { folder ->
                LibraryFolderItem(
                    id = folder.id,
                    name = folder.name
                )
            }
        }

    private val documents = query.flatMapLatest { currentQuery ->
        repository.observeLibrary(currentQuery).map { documentEntities ->
            documentEntities.map { document ->
                LibraryDocumentItem(
                    id = document.id,
                    title = document.title,
                    pageCount = document.pageCount,
                    folderId = document.folderId
                )
            }
        }
    }

    val uiState: StateFlow<LibraryUiState> = combine(
        query,
        selectedFolderId,
        folders,
        documents
    ) { currentQuery, currentFolderId, folderItems, documentItems ->
        val visibleDocuments = documentItems.filter { document ->
            currentFolderId == null || document.folderId == currentFolderId
        }

        LibraryUiState(
            query = currentQuery,
            folders = folderItems,
            selectedFolderId = currentFolderId,
            documents = visibleDocuments
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LibraryUiState()
        )

    fun onQueryChange(query: String) {
        this.query.update { query }
    }

    fun onFolderClick(folderId: Long?) {
        selectedFolderId.update { folderId }
    }

    companion object {
        fun factory(
            repository: DocumentRepository,
            folderDao: FolderDao
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return LibraryViewModel(repository, folderDao) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}
