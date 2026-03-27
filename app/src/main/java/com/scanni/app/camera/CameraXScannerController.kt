package com.scanni.app.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class CameraXScannerController(
    private val appContext: Context,
    private val outputDir: File,
    private val lifecycleOwnerProvider: () -> LifecycleOwner,
    private val captureWithCamera: suspend (File, File, LifecycleOwner) -> Unit = { originalFile, previewFile, lifecycleOwner ->
        val cameraProvider = ProcessCameraProvider.getInstance(appContext).await()
        val imageCapture = ImageCapture.Builder().build()
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            imageCapture
        )

        try {
            imageCapture.captureToFile(appContext, originalFile)
            originalFile.copyTo(previewFile, overwrite = true)
        } finally {
            cameraProvider.unbind(imageCapture)
        }
    }
) : ScannerCameraController {

    override suspend fun capturePage(): CapturedPageDraft {
        outputDir.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val originalFile = File(outputDir, "scan_${timestamp}_original.jpg")
        val previewFile = File(outputDir, "scan_${timestamp}_preview.jpg")
        captureWithCamera(
            originalFile,
            previewFile,
            lifecycleOwnerProvider()
        )

        return CapturedPageDraft(
            originalPath = originalFile.absolutePath,
            previewPath = previewFile.absolutePath,
            detectedCorners = listOf(
                0f, 0f,
                1f, 0f,
                1f, 1f,
                0f, 1f
            )
        )
    }

    companion object {
        fun writeSampleImage(file: File, backgroundColor: Int, accentColor: Int) {
            file.parentFile?.mkdirs()
            Bitmap.createBitmap(900, 1200, Bitmap.Config.ARGB_8888).apply {
                eraseColor(backgroundColor)
                for (x in 140 until 760) {
                    for (y in 180 until 1020) {
                        setPixel(x, y, accentColor)
                    }
                }
                file.outputStream().use { stream ->
                    compress(Bitmap.CompressFormat.JPEG, 95, stream)
                }
                recycle()
            }
        }
    }
}

private suspend fun ListenableFuture<ProcessCameraProvider>.await(): ProcessCameraProvider =
    suspendCancellableCoroutine { continuation ->
        addListener(
            {
                runCatching { get() }
                    .onSuccess(continuation::resume)
                    .onFailure(continuation::resumeWithException)
            },
            Runnable::run
        )
        continuation.invokeOnCancellation {
            cancel(true)
        }
    }

private suspend fun ImageCapture.captureToFile(context: Context, outputFile: File) =
    suspendCancellableCoroutine<Unit> { continuation ->
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    continuation.resume(Unit)
                }

                override fun onError(exception: ImageCaptureException) {
                    continuation.resumeWithException(exception)
                }
            }
        )
    }
