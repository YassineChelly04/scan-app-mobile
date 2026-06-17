package com.scanni.app.di

import android.content.Context
import com.scanni.app.core.text.DocumentNamer
import com.scanni.app.data.DocumentRepositoryImpl
import com.scanni.app.data.SettingsRepositoryImpl
import com.scanni.app.data.db.ScanniDatabase
import com.scanni.app.data.files.PageFileStore
import com.scanni.app.domain.ScanSession
import com.scanni.app.domain.model.OcrScript
import com.scanni.app.domain.ocr.OcrEngine
import com.scanni.app.domain.ocr.OcrScheduler
import com.scanni.app.domain.processing.PageProcessor
import com.scanni.app.domain.repo.DocumentRepository
import com.scanni.app.domain.repo.SettingsRepository
import com.scanni.app.domain.usecase.ExportDocumentUseCase
import com.scanni.app.domain.usecase.RunOcrUseCase
import com.scanni.app.domain.usecase.SaveScanUseCase
import com.scanni.app.domain.usecase.UpdatePageUseCase
import com.scanni.app.export.SearchablePdfWriter
import com.scanni.app.ocr.MlKitLatinOcrEngine
import com.scanni.app.ocr.OcrWorkerFactory
import com.scanni.app.ocr.TesseractArabicOcrEngine
import com.scanni.app.ocr.WorkManagerOcrScheduler
import com.scanni.app.vision.OpenCvDocumentDetector
import com.scanni.app.vision.OpenCvPageEnhancer

/**
 * Hand-wired composition root. Everything is a lazy singleton created on first
 * use; ViewModels receive dependencies through small factory functions in the UI
 * layer. Deliberate alternative to a DI framework — the graph is small enough to
 * read in one screen and there is zero annotation-processing build cost.
 */
class AppGraph(private val appContext: Context) {

    val fileStore: PageFileStore by lazy { PageFileStore(appContext) }

    val database: ScanniDatabase by lazy { ScanniDatabase.create(appContext) }

    val documentRepository: DocumentRepository by lazy {
        DocumentRepositoryImpl(database, fileStore, ocrScheduler)
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepositoryImpl(appContext) }

    val scanSession: ScanSession by lazy { ScanSession() }

    val documentDetector: OpenCvDocumentDetector by lazy { OpenCvDocumentDetector() }

    val pageProcessor: PageProcessor by lazy { OpenCvPageEnhancer() }

    val pdfWriter: SearchablePdfWriter by lazy { SearchablePdfWriter(appContext) }

    val documentNamer: DocumentNamer by lazy { DocumentNamer() }

    val ocrScheduler: OcrScheduler by lazy { WorkManagerOcrScheduler(appContext) }

    private val latinOcrEngine: OcrEngine by lazy { MlKitLatinOcrEngine() }
    private val arabicOcrEngine: OcrEngine by lazy { TesseractArabicOcrEngine(appContext) }

    val runOcrUseCase: RunOcrUseCase by lazy {
        RunOcrUseCase(documentRepository, settingsRepository, fileStore) { script ->
            when (script) {
                OcrScript.LATIN -> latinOcrEngine
                OcrScript.ARABIC -> arabicOcrEngine
            }
        }
    }

    val saveScanUseCase: SaveScanUseCase by lazy {
        SaveScanUseCase(documentRepository, fileStore, pageProcessor, ocrScheduler)
    }

    val updatePageUseCase: UpdatePageUseCase by lazy {
        UpdatePageUseCase(documentRepository, fileStore, pageProcessor, ocrScheduler)
    }

    val exportDocumentUseCase: ExportDocumentUseCase by lazy {
        ExportDocumentUseCase(documentRepository, fileStore, pdfWriter)
    }

    val workerFactory: OcrWorkerFactory by lazy { OcrWorkerFactory { runOcrUseCase } }
}
