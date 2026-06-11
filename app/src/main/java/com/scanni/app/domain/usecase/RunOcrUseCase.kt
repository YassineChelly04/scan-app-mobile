package com.scanni.app.domain.usecase

import android.util.Log
import com.scanni.app.data.files.PageFileStore
import com.scanni.app.domain.model.OcrScript
import com.scanni.app.domain.model.OcrStatus
import com.scanni.app.domain.model.OcrWords
import com.scanni.app.domain.ocr.OcrEngine
import com.scanni.app.domain.repo.DocumentRepository
import com.scanni.app.domain.repo.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Runs text recognition over every not-yet-recognized page of a document. */
class RunOcrUseCase(
    private val repository: DocumentRepository,
    private val settingsRepository: SettingsRepository,
    private val fileStore: PageFileStore,
    private val engineProvider: (OcrScript) -> OcrEngine,
) {
    /** @return true when every pending page was recognized successfully. */
    suspend operator fun invoke(documentId: String): Boolean = withContext(Dispatchers.Default) {
        val pages = repository.getPages(documentId).filter { it.ocrStatus != OcrStatus.DONE }
        if (pages.isEmpty()) return@withContext true
        val script = settingsRepository.current().ocrScript
        val engine = engineProvider(script)

        var allOk = true
        for (page in pages) {
            repository.setPageOcr(page.id, OcrStatus.RUNNING, null, null)
            val outcome = runCatching {
                val bitmap = fileStore.loadBitmap(page.processedPath, MAX_OCR_DIMENSION)
                try {
                    engine.recognize(bitmap)
                } finally {
                    bitmap.recycle()
                }
            }
            outcome.fold(
                onSuccess = { result ->
                    repository.setPageOcr(
                        page.id,
                        OcrStatus.DONE,
                        result.text,
                        OcrWords.encode(result.words),
                    )
                },
                onFailure = { error ->
                    Log.w(TAG, "OCR failed for page ${page.id}", error)
                    repository.setPageOcr(page.id, OcrStatus.FAILED, null, null)
                    allOk = false
                },
            )
        }
        allOk
    }

    private companion object {
        const val TAG = "RunOcrUseCase"
        const val MAX_OCR_DIMENSION = 2048
    }
}
