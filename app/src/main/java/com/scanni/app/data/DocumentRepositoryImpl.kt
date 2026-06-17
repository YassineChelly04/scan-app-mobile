package com.scanni.app.data

import androidx.room.withTransaction
import com.scanni.app.core.geometry.Quad
import com.scanni.app.core.text.FtsQuery
import com.scanni.app.data.db.DocumentEntity
import com.scanni.app.data.db.DocumentRow
import com.scanni.app.data.db.FolderEntity
import com.scanni.app.data.db.PageEntity
import com.scanni.app.data.db.ScanniDatabase
import com.scanni.app.data.files.PageFileStore
import com.scanni.app.domain.model.Document
import com.scanni.app.domain.model.Folder
import com.scanni.app.domain.model.OcrStatus
import com.scanni.app.domain.model.Page
import com.scanni.app.domain.model.PageDraft
import com.scanni.app.domain.model.ScanFilter
import com.scanni.app.domain.model.SearchHit
import com.scanni.app.domain.ocr.OcrScheduler
import com.scanni.app.domain.repo.DocumentRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class DocumentRepositoryImpl(
    private val database: ScanniDatabase,
    private val fileStore: PageFileStore,
    private val ocrScheduler: OcrScheduler,
    private val clock: () -> Long = System::currentTimeMillis,
) : DocumentRepository {

    private val documentDao = database.documentDao()
    private val pageDao = database.pageDao()
    private val folderDao = database.folderDao()
    private val ftsDao = database.pageFtsDao()

    override fun observeDocuments(folderId: String?): Flow<List<Document>> {
        val source = if (folderId == null) {
            documentDao.observeAll()
        } else {
            documentDao.observeInFolder(folderId)
        }
        return source.map { rows -> rows.map { it.toDomain() } }
    }

    override fun observeFolders(): Flow<List<Folder>> =
        folderDao.observeAll().map { rows ->
            rows.map { Folder(it.id, it.name, it.createdAt, it.documentCount) }
        }

    override fun observeDocument(documentId: String): Flow<Document?> =
        documentDao.observeById(documentId).map { it?.toDomain() }

    override fun observePages(documentId: String): Flow<List<Page>> =
        pageDao.observeByDocument(documentId).map { rows -> rows.map { it.toDomain() } }

    override fun search(query: String): Flow<List<SearchHit>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return flowOf(emptyList())

        val titleFlow = documentDao.observeTitleMatches(FtsQuery.likePattern(trimmed))
        val match = FtsQuery.sanitize(trimmed)
        val ftsFlow = if (match == null) flowOf(emptyList()) else ftsDao.observeMatches(match)

        return combine(titleFlow, ftsFlow) { titleRows, ftsHits ->
            val snippetByDoc = LinkedHashMap<String, String>()
            for (hit in ftsHits) {
                snippetByDoc.putIfAbsent(hit.documentId, hit.snippet)
            }
            val extraIds = snippetByDoc.keys - titleRows.map { it.id }.toSet()
            val extraRows = if (extraIds.isEmpty()) {
                emptyList()
            } else {
                documentDao.getManyByIds(extraIds.toList())
            }
            (titleRows + extraRows)
                .distinctBy { it.id }
                .sortedByDescending { it.updatedAt }
                .map { row -> SearchHit(row.toDomain(), snippetByDoc[row.id]) }
        }
    }

    override suspend fun getDocument(documentId: String): Document? =
        documentDao.getById(documentId)?.toDomain()

    override suspend fun getPages(documentId: String): List<Page> =
        pageDao.getByDocument(documentId).map { it.toDomain() }

    override suspend fun getPage(pageId: String): Page? = pageDao.getById(pageId)?.toDomain()

    override suspend fun createDocument(
        documentId: String,
        title: String,
        folderId: String?,
        pages: List<PageDraft>,
    ) = database.withTransaction {
        val now = clock()
        documentDao.insert(
            DocumentEntity(
                id = documentId,
                title = title,
                folderId = folderId,
                createdAt = now,
                updatedAt = now,
            ),
        )
        pageDao.insertAll(
            pages.map { draft ->
                PageEntity(
                    id = draft.id,
                    documentId = documentId,
                    position = draft.position,
                    originalPath = draft.originalPath,
                    processedPath = draft.processedPath,
                    thumbPath = draft.thumbPath,
                    widthPx = draft.widthPx,
                    heightPx = draft.heightPx,
                    quadEncoded = draft.quad?.encode(),
                    rotationDeg = draft.rotationDeg,
                    filter = draft.filter.name,
                    ocrStatus = OcrStatus.PENDING.name,
                    ocrText = null,
                    ocrWordsJson = null,
                    revision = 0,
                )
            },
        )
    }

    override suspend fun renameDocument(documentId: String, title: String) {
        documentDao.rename(documentId, title, clock())
    }

    override suspend fun moveDocuments(documentIds: List<String>, folderId: String?) {
        if (documentIds.isEmpty()) return
        documentDao.moveToFolder(documentIds, folderId, clock())
    }

    override suspend fun deleteDocuments(documentIds: List<String>) {
        if (documentIds.isEmpty()) return
        // Stop any in-flight recognition first so a worker can't write rows for, or
        // read files of, a document we are about to remove.
        documentIds.forEach { ocrScheduler.cancelDocument(it) }
        database.withTransaction {
            ftsDao.deleteForDocuments(documentIds)
            documentDao.delete(documentIds)
        }
        // Page rows cascade-delete with the document; FTS rows are removed above.
        documentIds.forEach { fileStore.deleteDocumentFiles(it) }
    }

    override suspend fun updatePageEdit(
        pageId: String,
        quad: Quad?,
        rotationDeg: Int,
        filter: ScanFilter,
        widthPx: Int,
        heightPx: Int,
    ) {
        val page = pageDao.getById(pageId) ?: return
        database.withTransaction {
            pageDao.applyEdit(
                id = pageId,
                quadEncoded = quad?.encode(),
                rotationDeg = rotationDeg,
                filter = filter.name,
                widthPx = widthPx,
                heightPx = heightPx,
                ocrStatus = OcrStatus.PENDING.name,
            )
            ftsDao.deleteForPage(pageId)
            documentDao.touch(page.documentId, clock())
        }
    }

    override suspend fun setPageOcr(
        pageId: String,
        status: OcrStatus,
        text: String?,
        wordsJson: String?,
    ) {
        val page = pageDao.getById(pageId) ?: return
        database.withTransaction {
            pageDao.setOcr(pageId, status.name, text, wordsJson)
            if (status == OcrStatus.DONE) {
                ftsDao.replaceForPage(pageId, page.documentId, text.orEmpty())
            }
        }
    }

    override suspend fun resetOcr(documentId: String) = database.withTransaction {
        pageDao.resetOcrForDocument(documentId, OcrStatus.PENDING.name)
        ftsDao.deleteForDocuments(listOf(documentId))
    }

    override suspend fun createFolder(name: String): String {
        val id = UUID.randomUUID().toString()
        folderDao.insert(FolderEntity(id = id, name = name, createdAt = clock()))
        return id
    }

    override suspend fun renameFolder(folderId: String, name: String) {
        folderDao.rename(folderId, name)
    }

    override suspend fun deleteFolder(folderId: String) {
        folderDao.delete(folderId)
    }

    private fun DocumentRow.toDomain() = Document(
        id = id,
        title = title,
        folderId = folderId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        pageCount = pageCount,
        thumbPath = thumbPath,
    )

    private fun PageEntity.toDomain() = Page(
        id = id,
        documentId = documentId,
        position = position,
        originalPath = originalPath,
        processedPath = processedPath,
        thumbPath = thumbPath,
        widthPx = widthPx,
        heightPx = heightPx,
        quad = Quad.decode(quadEncoded),
        rotationDeg = rotationDeg,
        filter = runCatching { ScanFilter.valueOf(filter) }.getOrDefault(ScanFilter.ORIGINAL),
        ocrStatus = runCatching { OcrStatus.valueOf(ocrStatus) }.getOrDefault(OcrStatus.PENDING),
        ocrText = ocrText,
        ocrWordsJson = ocrWordsJson,
    )
}
