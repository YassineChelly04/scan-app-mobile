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

    override suspend fun saveProcessedDocument(
        title: String,
        folderId: Long?,
        pageImageUris: List<String>
    ): Long {
        val documentId = createDocument(
            title = title,
            folderId = folderId,
            pageCount = pageImageUris.size
        )
        pageDao.insertAll(
            pageImageUris.mapIndexed { index, imageUri ->
                com.scanni.app.data.db.PageEntity(
                    documentId = documentId,
                    pageNumber = index + 1,
                    imageUri = imageUri
                )
            }
        )
        return documentId
    }

    override suspend fun savePageText(documentId: Long, pageIndex: Int, text: String) {
        pageTextDao.upsert(
            PageTextEntity(
                documentId = documentId,
                pageIndex = pageIndex,
                text = text
            )
        )
    }

    override suspend fun updateDocument(documentId: Long, title: String, folderId: Long?) {
        documentDao.updateDocument(
            documentId = documentId,
            title = title,
            folderId = folderId
        )
    }

    override suspend fun getExportableDocument(documentId: Long): ExportableDocument? {
        val document = documentDao.getById(documentId) ?: return null
        val pages = pageDao.getPagesForDocument(documentId)
        if (pages.size != document.pageCount) {
            return null
        }

        val pageImageUris = pages.map { page -> page.imageUri }
        if (pageImageUris.any(String::isBlank)) {
            return null
        }

        return ExportableDocument(
            id = document.id,
            title = document.title,
            pageCount = document.pageCount,
            ocrStatus = document.ocrStatus,
            folderId = document.folderId,
            pageImageUris = pageImageUris
        )
    }

    private companion object {
        const val DEFAULT_OCR_STATUS = "pending"
    }
}
