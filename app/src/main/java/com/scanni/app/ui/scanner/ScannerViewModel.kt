package com.scanni.app.ui.scanner

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanni.app.core.geometry.Quad
import com.scanni.app.core.geometry.QuadStabilizer
import com.scanni.app.core.image.ImageIo
import com.scanni.app.data.files.PageFileStore
import com.scanni.app.domain.ScanSession
import com.scanni.app.domain.model.CapturedPage
import com.scanni.app.domain.model.ScanMode
import com.scanni.app.domain.repo.SettingsRepository
import com.scanni.app.vision.OpenCvDocumentDetector
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ScannerUiState(
    val mode: ScanMode = ScanMode.DOCUMENT,
    val torchOn: Boolean = false,
    val pages: List<CapturedPage> = emptyList(),
    val detection: QuadStabilizer.State = QuadStabilizer.State.Searching,
    val frameWidth: Int = 3,
    val frameHeight: Int = 4,
    val autoCapture: Boolean = true,
    val capturing: Boolean = false,
)

sealed interface ScannerEvent {
    /** The screen should trigger ImageCapture now (auto or manual shutter). */
    data object Capture : ScannerEvent
    data object PageCaptured : ScannerEvent
    data object ImportFailed : ScannerEvent
}

class ScannerViewModel(
    private val session: ScanSession,
    private val settingsRepository: SettingsRepository,
    private val fileStore: PageFileStore,
    private val detector: OpenCvDocumentDetector,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) : ViewModel() {

    private val stabilizer = QuadStabilizer()
    private val stabilizerLock = Any()

    private data class CameraState(
        val torchOn: Boolean = false,
        val capturing: Boolean = false,
        val detection: QuadStabilizer.State = QuadStabilizer.State.Searching,
        val frameWidth: Int = 3,
        val frameHeight: Int = 4,
    )

    private val cameraState = MutableStateFlow(CameraState())
    private var cooldownUntil = 0L

    private val _events = MutableSharedFlow<ScannerEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<ScannerEvent> = _events

    val uiState: StateFlow<ScannerUiState> = combine(
        session.pages,
        session.mode,
        settingsRepository.settings,
        cameraState,
    ) { pages, mode, settings, camera ->
        ScannerUiState(
            mode = mode,
            torchOn = camera.torchOn,
            pages = pages,
            detection = camera.detection,
            frameWidth = camera.frameWidth,
            frameHeight = camera.frameHeight,
            autoCapture = settings.autoCapture,
            capturing = camera.capturing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(2_000), ScannerUiState())

    /** Detector callback — runs on the camera analysis thread. */
    fun onDetection(quad: Quad?, frameWidth: Int, frameHeight: Int) {
        val now = clock()
        val state = synchronized(stabilizerLock) { stabilizer.onFrame(quad, now) }
        cameraState.update { camera ->
            camera.copy(
                detection = state,
                frameWidth = if (frameWidth > 0) frameWidth else camera.frameWidth,
                frameHeight = if (frameHeight > 0) frameHeight else camera.frameHeight,
            )
        }
        val current = uiState.value
        if (
            state is QuadStabilizer.State.Locked &&
            current.autoCapture &&
            current.mode.detectionEnabled &&
            now >= cooldownUntil
        ) {
            requestCapture()
        }
    }

    /** Triggers a capture (shutter button or auto-capture lock). */
    fun requestCapture() {
        var fire = false
        cameraState.update { camera ->
            if (camera.capturing) {
                camera
            } else {
                fire = true
                camera.copy(capturing = true)
            }
        }
        if (fire) _events.tryEmit(ScannerEvent.Capture)
    }

    fun onCaptured(path: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val mode = session.mode.value
            val page = buildPage(path, mode)
            session.add(page)
            cooldownUntil = clock() + CAPTURE_COOLDOWN_MS
            synchronized(stabilizerLock) { stabilizer.rearm() }
            cameraState.update { it.copy(capturing = false) }
            _events.tryEmit(ScannerEvent.PageCaptured)
        }
    }

    fun onCaptureError() {
        cameraState.update { it.copy(capturing = false) }
    }

    fun setMode(mode: ScanMode) {
        session.setMode(mode)
        synchronized(stabilizerLock) { stabilizer.reset() }
        cameraState.update { it.copy(detection = QuadStabilizer.State.Searching) }
    }

    fun toggleTorch() {
        cameraState.update { it.copy(torchOn = !it.torchOn) }
    }

    /** Copies picked gallery images into the session. @return true when at least one imported. */
    suspend fun importImages(context: Context, uris: List<Uri>): Boolean =
        withContext(Dispatchers.IO) {
            val mode = session.mode.value
            var imported = 0
            for (uri in uris) {
                runCatching {
                    val target = fileStore.newSessionFile()
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    } ?: error("Cannot open $uri")
                    session.add(buildPage(target.absolutePath, mode))
                    imported++
                }
            }
            if (imported == 0 && uris.isNotEmpty()) _events.tryEmit(ScannerEvent.ImportFailed)
            imported > 0
        }

    private fun buildPage(path: String, mode: ScanMode): CapturedPage {
        val (width, height) = ImageIo.orientedSize(path)
        val detected = if (mode.detectionEnabled) {
            runCatching {
                val bitmap = ImageIo.decodeOriented(path, DETECT_DIMENSION)
                try {
                    detector.detect(bitmap, mode.minAreaFraction)
                } finally {
                    bitmap.recycle()
                }
            }.getOrNull()
        } else {
            null
        }
        return CapturedPage(
            id = UUID.randomUUID().toString(),
            originalPath = path,
            widthPx = width,
            heightPx = height,
            detectedQuad = detected,
            quad = detected ?: Quad.FULL,
            rotationDeg = 0,
            filter = mode.defaultFilter,
        )
    }

    private companion object {
        const val CAPTURE_COOLDOWN_MS = 2_000L
        const val DETECT_DIMENSION = 1280
    }
}
