package com.scanni.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Document row joined with derived page metadata. */
data class DocumentRow(
    val id: String,
    val title: String,
    val folderId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val pageCount: Int,
    val thumbPath: String?,
)

data class FolderRow(
    val id: String,
    val name: String,
    val createdAt: Long,
    val documentCount: Int,
)

data class FtsHit(
    val documentId: String,
    val snippet: String,
)

private const val DOCUMENT_ROW_SELECT = """
    SELECT d.id, d.title, d.folderId, d.createdAt, d.updatedAt,
        (SELECT COUNT(*) FROM pages p WHERE p.documentId = d.id) AS pageCount,
        (SELECT p.thumbPath FROM pages p WHERE p.documentId = d.id
            ORDER BY p.position ASC LIMIT 1) AS thumbPath
    FROM documents d
"""

@Dao
interface DocumentDao {

    @Query("$DOCUMENT_ROW_SELECT ORDER BY d.updatedAt DESC")
    fun observeAll(): Flow<List<DocumentRow>>

    @Query("$DOCUMENT_ROW_SELECT WHERE d.folderId = :folderId ORDER BY d.updatedAt DESC")
    fun observeInFolder(folderId: String): Flow<List<DocumentRow>>

    @Query("$DOCUMENT_ROW_SELECT WHERE d.id = :id")
    fun observeById(id: String): Flow<DocumentRow?>

    @Query("$DOCUMENT_ROW_SELECT WHERE d.id = :id")
    suspend fun getById(id: String): DocumentRow?

    @Query("$DOCUMENT_ROW_SELECT WHERE d.id IN (:ids)")
    fun observeByIds(ids: List<String>): Flow<List<DocumentRow>>

    @Query(
        "$DOCUMENT_ROW_SELECT WHERE d.title LIKE :pattern ESCAPE '\\' ORDER BY d.updatedAt DESC",
    )
    fun observeTitleMatches(pattern: String): Flow<List<DocumentRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: DocumentEntity)

    @Query("UPDATE documents SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun rename(id: String, title: String, updatedAt: Long)

    @Query("UPDATE documents SET folderId = :folderId, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun moveToFolder(ids: List<String>, folderId: String?, updatedAt: Long)

    @Query("UPDATE documents SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun touch(id: String, updatedAt: Long)

    @Query("DELETE FROM documents WHERE id IN (:ids)")
    suspend fun delete(ids: List<String>)
}

@Dao
interface PageDao {

    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY position ASC")
    fun observeByDocument(documentId: String): Flow<List<PageEntity>>

    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY position ASC")
    suspend fun getByDocument(documentId: String): List<PageEntity>

    @Query("SELECT * FROM pages WHERE id = :id")
    suspend fun getById(id: String): PageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pages: List<PageEntity>)

    @Query(
        """
        UPDATE pages SET quadEncoded = :quadEncoded, rotationDeg = :rotationDeg,
            filter = :filter, widthPx = :widthPx, heightPx = :heightPx,
            ocrStatus = :ocrStatus, ocrText = NULL, ocrWordsJson = NULL,
            revision = revision + 1
        WHERE id = :id
        """,
    )
    suspend fun applyEdit(
        id: String,
        quadEncoded: String?,
        rotationDeg: Int,
        filter: String,
        widthPx: Int,
        heightPx: Int,
        ocrStatus: String,
    )

    @Query(
        """
        UPDATE pages SET ocrStatus = :status, ocrText = :text, ocrWordsJson = :wordsJson
        WHERE id = :id
        """,
    )
    suspend fun setOcr(id: String, status: String, text: String?, wordsJson: String?)

    @Query(
        "UPDATE pages SET ocrStatus = :status, ocrText = NULL, ocrWordsJson = NULL WHERE documentId = :documentId",
    )
    suspend fun resetOcrForDocument(documentId: String, status: String)
}

@Dao
interface FolderDao {

    @Query(
        """
        SELECT f.id, f.name, f.createdAt,
            (SELECT COUNT(*) FROM documents d WHERE d.folderId = f.id) AS documentCount
        FROM folders f ORDER BY f.name COLLATE NOCASE ASC
        """,
    )
    fun observeAll(): Flow<List<FolderRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: FolderEntity)

    @Query("UPDATE folders SET name = :name WHERE id = :id")
    suspend fun rename(id: String, name: String)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface PageFtsDao {

    @Query("DELETE FROM page_fts WHERE pageId = :pageId")
    suspend fun deleteForPage(pageId: String)

    @Insert
    suspend fun insert(row: PageFtsEntity)

    @Transaction
    suspend fun replaceForPage(pageId: String, documentId: String, content: String) {
        deleteForPage(pageId)
        if (content.isNotBlank()) {
            insert(PageFtsEntity(documentId = documentId, pageId = pageId, content = content))
        }
    }

    @Query("DELETE FROM page_fts WHERE documentId IN (:documentIds)")
    suspend fun deleteForDocuments(documentIds: List<String>)

    @Query(
        """
        SELECT documentId, snippet(page_fts, '<b>', '</b>', '…') AS snippet
        FROM page_fts WHERE page_fts MATCH :match
        """,
    )
    fun observeMatches(match: String): Flow<List<FtsHit>>
}
