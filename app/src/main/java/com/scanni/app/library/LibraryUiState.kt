package com.scanni.app.library

data class LibraryDocumentItem(
    val id: Long,
    val title: String,
    val pageCount: Int,
    val folderId: Long? = null
)

data class LibraryFolderItem(
    val id: Long,
    val name: String
)

data class LibraryUiState(
    val query: String = "",
    val folders: List<LibraryFolderItem> = emptyList(),
    val selectedFolderId: Long? = null,
    val documents: List<LibraryDocumentItem> = emptyList()
)
