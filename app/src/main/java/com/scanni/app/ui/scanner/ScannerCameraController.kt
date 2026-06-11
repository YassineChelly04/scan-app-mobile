package com.scanni.app.ui.scanner

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** Owns the CameraX use cases for the scanner screen. */
class ScannerCameraController {

    private var provider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    @SuppressLint("ClickableViewAccessibility")
    suspend fun bind(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        analyzer: ImageAnalysis.Analyzer,
    ) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).await(context)
        provider = cameraProvider

        val aspect = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .build()

        val preview = Preview.Builder()
            .setResolutionSelector(aspect)
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val analysis = ImageAnalysis.Builder()
            .setResolutionSelector(aspect)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(analysisExecutor, analyzer) }

        val capture = ImageCapture.Builder()
            .setResolutionSelector(aspect)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
        imageCapture = capture

        cameraProvider.unbindAll()
        camera = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            capture,
            analysis,
        )

        previewView.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                focusAt(previewView, event.x, event.y)
                view.performClick()
            }
            true
        }
    }

    fun takePicture(
        context: Context,
        target: File,
        onSaved: (File) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val capture = imageCapture ?: return onError(IllegalStateException("Camera not bound"))
        val options = ImageCapture.OutputFileOptions.Builder(target).build()
        capture.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onSaved(target)
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }
            },
        )
    }

    fun setTorch(enabled: Boolean) {
        camera?.cameraControl?.enableTorch(enabled)
    }

    private fun focusAt(previewView: PreviewView, x: Float, y: Float) {
        val cam = camera ?: return
        val point = previewView.meteringPointFactory.createPoint(x, y)
        cam.cameraControl.startFocusAndMetering(FocusMeteringAction.Builder(point).build())
    }

    fun release() {
        runCatching { provider?.unbindAll() }
        analysisExecutor.shutdown()
        camera = null
        imageCapture = null
        provider = null
    }
}

private suspend fun <T> ListenableFuture<T>.await(context: Context): T =
    suspendCancellableCoroutine { continuation ->
        addListener(
            {
                try {
                    continuation.resume(get())
                } catch (t: Throwable) {
                    continuation.resumeWithException(t)
                }
            },
            ContextCompat.getMainExecutor(context),
        )
        continuation.invokeOnCancellation { cancel(false) }
    }
