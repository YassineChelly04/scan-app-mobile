package com.scanni.app.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scanni.app.R
import com.scanni.app.core.geometry.OverlayTransform
import com.scanni.app.core.geometry.Quad
import com.scanni.app.core.geometry.QuadStabilizer
import com.scanni.app.di.AppGraph
import com.scanni.app.domain.model.ScanMode
import com.scanni.app.ui.common.PageImage
import com.scanni.app.ui.common.graphViewModel
import com.scanni.app.vision.CameraFrameAnalyzer
import kotlinx.coroutines.launch

@Composable
fun ScannerScreen(
    graph: AppGraph,
    onClose: () -> Unit,
    onReview: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel = graphViewModel {
        ScannerViewModel(
            session = graph.scanSession,
            settingsRepository = graph.settingsRepository,
            fileStore = graph.fileStore,
            detector = graph.documentDetector,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (hasPermission) {
            CameraContent(
                graph = graph,
                viewModel = viewModel,
                state = state,
                onReview = onReview,
            )
        } else {
            PermissionRationale(onGrant = { permissionLauncher.launch(Manifest.permission.CAMERA) })
        }

        // Top bar: close + torch.
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScannerIconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, stringResource(R.string.cd_close), tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            if (hasPermission) {
                ScannerIconButton(onClick = viewModel::toggleTorch) {
                    Icon(
                        imageVector = if (state.torchOn) {
                            Icons.Outlined.FlashOn
                        } else {
                            Icons.Outlined.FlashOff
                        },
                        contentDescription = stringResource(
                            if (state.torchOn) R.string.scanner_cd_flash_on else R.string.scanner_cd_flash_off,
                        ),
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraContent(
    graph: AppGraph,
    viewModel: ScannerViewModel,
    state: ScannerUiState,
    onReview: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val controller = remember { ScannerCameraController() }
    val analyzer = remember {
        CameraFrameAnalyzer(
            detector = graph.documentDetector,
            isEnabled = { viewModel.uiState.value.mode.detectionEnabled },
            minAreaFraction = { viewModel.uiState.value.mode.minAreaFraction },
            onResult = viewModel::onDetection,
        )
    }

    LaunchedEffect(Unit) {
        controller.bind(context, lifecycleOwner, previewView, analyzer)
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { controller.release() }
    }
    LaunchedEffect(state.torchOn) { controller.setTorch(state.torchOn) }

    val isLocked = state.detection is QuadStabilizer.State.Locked
    LaunchedEffect(isLocked) {
        if (isLocked) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val flashAlpha = remember { Animatable(0f) }
    val importFailedMessage = stringResource(R.string.scanner_import_failed)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ScannerEvent.Capture -> controller.takePicture(
                    context = context,
                    target = graph.fileStore.newSessionFile(),
                    onSaved = { file -> viewModel.onCaptured(file.absolutePath) },
                    onError = { viewModel.onCaptureError() },
                )

                ScannerEvent.PageCaptured -> {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    launch {
                        flashAlpha.snapTo(0.65f)
                        flashAlpha.animateTo(0f, tween(420))
                    }
                }

                ScannerEvent.ImportFailed ->
                    Toast.makeText(context, importFailedMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_GALLERY_PICKS),
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                if (viewModel.importImages(context, uris)) onReview()
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        if (state.mode.detectionEnabled) {
            QuadOverlay(
                detection = state.detection,
                frameWidth = state.frameWidth,
                frameHeight = state.frameHeight,
                modifier = Modifier.fillMaxSize(),
            )
            DetectionHint(
                detection = state.detection,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 72.dp),
            )
        }

        if (flashAlpha.value > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = flashAlpha.value)),
            )
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ModeSelector(
                selected = state.mode,
                onSelect = viewModel::setMode,
            )
            Spacer(Modifier.size(18.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ScannerIconButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly,
                            ),
                        )
                    },
                ) {
                    Icon(
                        Icons.Outlined.PhotoLibrary,
                        stringResource(R.string.scanner_cd_gallery),
                        tint = Color.White,
                    )
                }
                Spacer(Modifier.weight(1f))
                ShutterButton(
                    detection = state.detection,
                    capturing = state.capturing,
                    onClick = viewModel::requestCapture,
                )
                Spacer(Modifier.weight(1f))
                PageStackButton(
                    pages = state.pages,
                    onClick = onReview,
                )
            }
        }
    }
}

@Composable
private fun QuadOverlay(
    detection: QuadStabilizer.State,
    frameWidth: Int,
    frameHeight: Int,
    modifier: Modifier = Modifier,
) {
    val quad: Quad? = when (detection) {
        is QuadStabilizer.State.Tracking -> detection.quad
        is QuadStabilizer.State.Locked -> detection.quad
        QuadStabilizer.State.Searching -> null
    }
    val locked = detection is QuadStabilizer.State.Locked
    val accent = MaterialTheme.colorScheme.tertiary

    Canvas(modifier) {
        val q = quad ?: return@Canvas
        if (frameWidth <= 0 || frameHeight <= 0) return@Canvas
        val transform = OverlayTransform(
            imageWidth = frameWidth.toFloat(),
            imageHeight = frameHeight.toFloat(),
            viewWidth = size.width,
            viewHeight = size.height,
            mode = OverlayTransform.ScaleMode.FILL,
        )
        val corners = q.corners.map { corner ->
            val v = transform.imageToView(corner)
            Offset(v.x, v.y)
        }
        val path = Path().apply {
            moveTo(corners[0].x, corners[0].y)
            for (i in 1 until corners.size) lineTo(corners[i].x, corners[i].y)
            close()
        }
        val lineColor = if (locked) accent else Color.White
        drawPath(path, color = lineColor.copy(alpha = if (locked) 0.28f else 0.14f))
        drawPath(path, color = lineColor, style = Stroke(width = 2.5.dp.toPx()))
        corners.forEach { corner ->
            drawCircle(Color.White, radius = 7.dp.toPx(), center = corner)
            drawCircle(lineColor, radius = 4.5.dp.toPx(), center = corner)
        }
    }
}

@Composable
private fun DetectionHint(
    detection: QuadStabilizer.State,
    modifier: Modifier = Modifier,
) {
    val text = when (detection) {
        QuadStabilizer.State.Searching -> stringResource(R.string.scanner_hint_searching)
        is QuadStabilizer.State.Tracking -> stringResource(R.string.scanner_hint_hold_steady)
        is QuadStabilizer.State.Locked -> stringResource(R.string.scanner_hint_captured)
    }
    Surface(
        color = Color.Black.copy(alpha = 0.55f),
        shape = CircleShape,
        modifier = modifier,
    ) {
        AnimatedContent(targetState = text, label = "scanHint") { hint ->
            Text(
                text = hint,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ModeSelector(
    selected: ScanMode,
    onSelect: (ScanMode) -> Unit,
) {
    Row(
        Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ScanMode.entries.forEach { mode ->
            val isSelected = mode == selected
            val pillColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Black.copy(alpha = 0.45f)
                },
                animationSpec = tween(200),
                label = "modePill",
            )
            Surface(
                color = pillColor,
                shape = CircleShape,
                modifier = Modifier.clickable { onSelect(mode) },
            ) {
                Text(
                    text = stringResource(mode.labelRes()),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                )
            }
        }
    }
}

private fun ScanMode.labelRes(): Int = when (this) {
    ScanMode.DOCUMENT -> R.string.scanner_mode_document
    ScanMode.WHITEBOARD -> R.string.scanner_mode_whiteboard
    ScanMode.BUSINESS_CARD -> R.string.scanner_mode_card
    ScanMode.PHOTO -> R.string.scanner_mode_photo
}

@Composable
private fun ShutterButton(
    detection: QuadStabilizer.State,
    capturing: Boolean,
    onClick: () -> Unit,
) {
    val progress = when (detection) {
        is QuadStabilizer.State.Tracking -> detection.steadyProgress
        is QuadStabilizer.State.Locked -> 1f
        QuadStabilizer.State.Searching -> 0f
    }
    val scale by animateFloatAsState(
        targetValue = if (capturing) 0.86f else 1f,
        animationSpec = tween(140),
        label = "shutterScale",
    )
    val ringColor = MaterialTheme.colorScheme.tertiary
    val density = LocalDensity.current
    val captureLabel = stringResource(R.string.scanner_cd_capture)

    Box(
        modifier = Modifier
            .size(82.dp)
            .scale(scale)
            .clip(CircleShape)
            .clickable(enabled = !capturing, onClick = onClick)
            .semantics { contentDescription = captureLabel },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = with(density) { 4.dp.toPx() }
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = size.minDimension / 2 - stroke / 2,
                style = Stroke(stroke),
            )
            if (progress > 0f) {
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    style = Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                )
            }
        }
        Box(
            Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Composable
private fun PageStackButton(
    pages: List<com.scanni.app.domain.model.CapturedPage>,
    onClick: () -> Unit,
) {
    Box(Modifier.width(56.dp), contentAlignment = Alignment.Center) {
        if (pages.isEmpty()) return@Box
        Box(
            Modifier
                .size(52.dp)
                .clip(MaterialTheme.shapes.small)
                .clickable(onClick = onClick),
        ) {
            PageImage(
                path = pages.last().originalPath,
                modifier = Modifier.fillMaxSize(),
                contentDescription = stringResource(R.string.scanner_cd_review_pages),
            )
            Badge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp),
            ) {
                Text(pages.size.toString())
            }
        }
    }
}

@Composable
private fun ScannerIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f)),
    ) {
        content()
    }
}

@Composable
private fun PermissionRationale(onGrant: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.scanner_permission_title),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
        )
        Spacer(Modifier.size(10.dp))
        Text(
            text = stringResource(R.string.scanner_permission_body),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.size(20.dp))
        Button(onClick = onGrant) {
            Text(stringResource(R.string.scanner_permission_grant))
        }
    }
}

private const val MAX_GALLERY_PICKS = 20
