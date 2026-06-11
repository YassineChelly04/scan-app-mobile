package com.scanni.app.domain.repo

import com.scanni.app.core.geometry.Quad
import com.scanni.app.domain.model.Document
import com.scanni.app.domain.model.Folder
import com.scanni.app.domain.model.OcrStatus
import com.scanni.app.domain.model.Page
import com.scanni.app.domain.model.PageDraft
import com.scanni.app.domain.model.ScanFilter
import com.scanni.app.domain.model.SearchHit
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {

    fun observeDocuments(folderId: String?): Flow<List<Document>>

    fun observeFolders(): Flow<List<Folder>>

    fun observeDocument(documentId: String): Flow<Document?>

    fun observePages(documentId: String): Flow<List<Page>>

    fun search(query: String): Flow<List<SearchHit>>

    suspend fun getDocument(documentId: String): Document?

    suspend fun getPages(documentId: String): List<Page>

    suspend fun getPage(pageId: String): Page?

    suspend fun createDocument(
        documentId: String,
        title: String,
        folderId: String?,
        pages: List<PageDraft>,
    )

    suspend fun renameDocument(documentId: String, title: String)

    suspend fun moveDocuments(documentIds: List<String>, folderId: String?)

    suspend fun deleteDocuments(documentIds: List<String>)

    suspend fun updatePageEdit(
        pageId: String,
        quad: Quad?,
        rotationDeg: Int,
        filter: ScanFilter,
        widthPx: Int,
        heightPx: Int,
    )

    suspend fun setPageOcr(pageId: String, status: OcrStatus, text: String?, wordsJson: String?)

    suspend fun resetOcr(documentId: String)

    suspend fun createFolder(name: String): String

    suspend fun renameFolder(folderId: String, name: String)

    suspend fun deleteFolder(folderId: String)
}
