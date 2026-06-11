package com.scanni.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
)

@Entity(
    tableName = "documents",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("folderId"), Index("updatedAt")],
)
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val folderId: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "pages",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("documentId")],
)
data class PageEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val position: Int,
    val originalPath: String,
    val processedPath: String,
    val thumbPath: String,
    val widthPx: Int,
    val heightPx: Int,
    val quadEncoded: String?,
    val rotationDeg: Int,
    val filter: String,
    val ocrStatus: String,
    val ocrText: String?,
    val ocrWordsJson: String?,
    /** Bumped on every re-render so image caches invalidate. */
    val revision: Int,
)

/** Full-text index over recognized page text. */
@Fts4(notIndexed = ["documentId", "pageId"])
@Entity(tableName = "page_fts")
data class PageFtsEntity(
    val documentId: String,
    val pageId: String,
    val content: String,
)
