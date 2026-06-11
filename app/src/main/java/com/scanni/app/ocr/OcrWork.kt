package com.scanni.app.ocr

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.scanni.app.domain.ocr.OcrScheduler
import com.scanni.app.domain.usecase.RunOcrUseCase

/** Background text recognition for one document. */
class OcrWorker(
    appContext: Context,
    params: WorkerParameters,
    private val runOcr: RunOcrUseCase,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val documentId = inputData.getString(KEY_DOCUMENT_ID) ?: return Result.failure()
        val ok = runCatching { runOcr(documentId) }.getOrDefault(false)
        return when {
            ok -> Result.success()
            runAttemptCount < MAX_ATTEMPTS -> Result.retry()
            else -> Result.failure()
        }
    }

    companion object {
        const val KEY_DOCUMENT_ID = "documentId"
        private const val MAX_ATTEMPTS = 2

        fun uniqueName(documentId: String) = "ocr_$documentId"
    }
}

class OcrWorkerFactory(
    private val runOcrProvider: () -> RunOcrUseCase,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? = when (workerClassName) {
        OcrWorker::class.java.name -> OcrWorker(appContext, workerParameters, runOcrProvider())
        else -> null
    }
}

class WorkManagerOcrScheduler(private val context: Context) : OcrScheduler {
    override fun scheduleDocument(documentId: String) {
        val request = OneTimeWorkRequestBuilder<OcrWorker>()
            .setInputData(workDataOf(OcrWorker.KEY_DOCUMENT_ID to documentId))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            OcrWorker.uniqueName(documentId),
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request,
        )
    }
}
