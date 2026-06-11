package com.scanni.app.data.files

import android.content.Context
import android.graphics.Bitmap
import com.scanni.app.core.image.ImageIo
import com.scanni.app.domain.processing.PageProcessor
import java.io.File
import java.util.UUID

/**
 * Owns every image file Scanni writes:
 *  - `cache/scan_session/` originals captured during an in-progress scan
 *  - `files/documents/<docId>/` originals + processed pages + thumbnails of saved documents
 *  - `cache/exports/` generated PDFs handed to the share sheet
 */
class PageFileStore(private val context: Context) {

    private val documentsDir get() = File(context.filesDir, "documents")
    private val sessionDir get() = File(context.cacheDir, "scan_session")
    private val exportsDir get() = File(context.cacheDir, "exports")
    private val previewsDir get() = File(context.cacheDir, "previews")

    // --- Scan session ---

    fun newSessionFile(): File {
        sessionDir.mkdirs()
        return File(sessionDir, "${UUID.randomUUID()}.jpg")
    }

    fun deleteSessionFile(path: String) {
        runCatching { File(path).delete() }
    }

    fun clearSession() {
        sessionDir.listFiles()?.forEach { runCatching { it.delete() } }
    }

    // --- Render previews (review/edit screens) ---

    fun previewFile(name: String): File {
        previewsDir.mkdirs()
        return File(previewsDir, "$name.jpg")
    }

    fun clearPreviews() {
        previewsDir.listFiles()?.forEach { runCatching { it.delete() } }
    }

    // --- Saved documents ---

    fun persistOriginal(documentId: String, pageId: String, sessionPath: String): File {
        val target = File(pageDir(documentId), "${pageId}_original.jpg")
        val source = File(sessionPath)
        if (!source.renameTo(target)) {
            source.copyTo(target, overwrite = true)
            source.delete()
        }
        return target
    }

    fun writeProcessed(documentId: String, pageId: String, bitmap: Bitmap): File {
        val target = File(pageDir(documentId), "${pageId}_processed.jpg")
        ImageIo.saveJpeg(bitmap, target, quality = 92)
        return target
    }

    fun writeThumb(documentId: String, pageId: String, processed: Bitmap): File {
        val target = File(pageDir(documentId), "${pageId}_thumb.jpg")
        writeThumbTo(target, processed)
        return target
    }

    fun overwriteProcessed(processedPath: String, bitmap: Bitmap) {
        ImageIo.saveJpeg(bitmap, File(processedPath), quality = 92)
    }

    fun overwriteThumb(thumbPath: String, processed: Bitmap) {
        writeThumbTo(File(thumbPath), processed)
    }

    fun deleteDocumentFiles(documentId: String) {
        runCatching { File(documentsDir, documentId).deleteRecursively() }
    }

    fun loadBitmap(path: String, maxDimension: Int): Bitmap =
        ImageIo.decodeOriented(path, maxDimension)

    // --- Exports ---

    fun newExportFile(baseName: String, extension: String): File {
        exportsDir.mkdirs()
        pruneOldExports()
        var candidate = File(exportsDir, "$baseName.$extension")
        var counter = 2
        while (candidate.exists()) {
            candidate = File(exportsDir, "$baseName ($counter).$extension")
            counter++
        }
        return candidate
    }

    private fun pruneOldExports() {
        val cutoff = System.currentTimeMillis() - EXPORT_TTL_MS
        exportsDir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoff) runCatching { file.delete() }
        }
    }

    private fun pageDir(documentId: String): File =
        File(documentsDir, documentId).apply { mkdirs() }

    private fun writeThumbTo(target: File, processed: Bitmap) {
        val scale = PageProcessor.THUMB_SIZE.toFloat() / maxOf(processed.width, processed.height)
        val thumb = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                processed,
                (processed.width * scale).toInt().coerceAtLeast(1),
                (processed.height * scale).toInt().coerceAtLeast(1),
                true,
            )
        } else {
            processed
        }
        ImageIo.saveJpeg(thumb, target, quality = 82)
        if (thumb !== processed) thumb.recycle()
    }

    private companion object {
        const val EXPORT_TTL_MS = 24L * 60 * 60 * 1000
    }
}
