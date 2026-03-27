package com.scanni.app.data.repo

import com.scanni.app.data.db.DocumentDao
import com.scanni.app.data.db.DocumentEntity
import com.scanni.app.data.db.PageDao
import com.scanni.app.data.db.PageTextDao
import com.scanni.app.data.db.PageTextEntity
import com.scanni.app.document.ExportableDocument
import kotlinx.coroutines.flow.Flow

class LocalDocumentRepository(
    private val documentDao: DocumentDao,
    private val pageTextDao: PageTextDao,
    private val pageDao: PageDao
) : DocumentRepository {
    override fun observeLibrary(query: String): Flow<List<DocumentEntity>> =
        documentDao.observeLibrary(query)

    override suspend fun createDocument(title: String, folderId: Long?, pageCount: Int): Long =
        documentDao.insert(
            DocumentEntity(
                title = title,
                folderId = folderId,
                pageCount = pageCount,
                ocrStatus = DEFAULT_OCR_STATUS
            )
        )

    override suspend fun savePageText(documentId: Long, pageIndex: Int, text: String) {
        pageTextDao.upsert(
            PageTextEntity(
                documentId = documentId,
                pageIndex = pageIndex,
                text = text
            )
        )
    }

    override suspend fun getExportableDocument(documentId: Long): ExportableDocument? {
        val document = documentDao.getById(documentId) ?: return null
        val pages = pageDao.getPagesForDocument(documentId)

        return ExportableDocument(
            id = document.id,
            title = document.title,
            pageCount = document.pageCount,
            ocrStatus = document.ocrStatus,
            pageImageUris = pages.map { page -> page.imageUri }
        )
    }

    private companion object {
        const val DEFAULT_OCR_STATUS = "pending"
    }
}
