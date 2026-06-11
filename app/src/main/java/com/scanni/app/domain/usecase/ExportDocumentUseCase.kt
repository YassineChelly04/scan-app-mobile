package com.scanni.app.domain.usecase

import com.scanni.app.data.files.PageFileStore
import com.scanni.app.domain.repo.DocumentRepository
import com.scanni.app.export.SearchablePdfWriter
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExportDocumentUseCase(
    private val repository: DocumentRepository,
    private val fileStore: PageFileStore,
    private val pdfWriter: SearchablePdfWriter,
) {
    /** Builds a searchable PDF in the exports cache and returns it. */
    suspend fun exportPdf(documentId: String): File = withContext(Dispatchers.IO) {
        val document = requireNotNull(repository.getDocument(documentId)) { "Document missing" }
        val pages = repository.getPages(documentId)
        require(pages.isNotEmpty()) { "Document has no pages" }
        val outFile = fileStore.newExportFile(sanitizeFileName(document.title), "pdf")
        pdfWriter.write(outFile, pages)
        outFile
    }

    suspend fun imageFiles(documentId: String): List<File> = withContext(Dispatchers.IO) {
        repository.getPages(documentId).map { File(it.processedPath) }.filter { it.exists() }
    }

    suspend fun allText(documentId: String): String = withContext(Dispatchers.IO) {
        repository.getPages(documentId)
            .mapNotNull { it.ocrText?.takeIf(String::isNotBlank) }
            .joinToString("\n\n")
    }

    companion object {
        fun sanitizeFileName(title: String): String {
            val cleaned = title
                .replace(Regex("[\\\\/:*?\"<>|\\n\\r\\t]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
                .take(60)
            return cleaned.ifEmpty { "Scanni document" }
        }
    }
}
