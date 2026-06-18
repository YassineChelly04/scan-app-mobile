package com.scanni.app.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.scanni.app.R
import com.scanni.app.core.geometry.CropGeometry
import com.scanni.app.core.geometry.OverlayTransform
import com.scanni.app.core.geometry.Quad
import com.scanni.app.core.geometry.Vec2
import com.scanni.app.core.image.ImageIo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Full-screen manual crop: drag corners or edge midpoints of the quad over the
 * original photo, with a magnifier loupe while dragging for pixel-accurate
 * corner placement.
 */
@Composable
fun CropEditor(
    imagePath: String,
    initialQuad: Quad,
    detectedQuad: Quad?,
    onApply: (Quad) -> Unit,
    onCancel: () -> Unit,
    detect: (suspend () -> Quad?)? = null,
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, imagePath) {
        value = withContext(Dispatchers.Default) {
            runCatching {
                ImageIo.decodeOriented(imagePath, EDITOR_IMAGE_SIZE).asImageBitmap()
            }.getOrNull()
        }
    }

    // rememberSaveable so an in-progress crop survives configuration changes
    // (e.g. rotation). Keyed on imagePath so a new image resets to its initialQuad.
    var quad by rememberSaveable(imagePath, stateSaver = QuadSaver) {
        mutableStateOf(initialQuad)
    }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var activeHandle by remember { mutableStateOf(CropGeometry.HANDLE_NONE) }
    var touchPosition by remember { mutableStateOf(Offset.Zero) }
    var userEdited by remember(imagePath) { mutableStateOf(false) }
    var snapEngaged by remember(imagePath) { mutableStateOf(false) }

    // On-demand document detection so the crop assistant can (re)find the paper
    // even when capture-time detection missed it.
    var detecting by remember(imagePath) { mutableStateOf(detect != null) }
    var liveDetected by remember(imagePath) { mutableStateOf<Quad?>(null) }
    // Keyed on imagePath only — the caller passes a fresh lambda each recomposition,
    // and the path is what actually identifies the image to detect.
    LaunchedEffect(imagePath) {
        if (detect == null) return@LaunchedEffect
        detecting = true
        liveDetected = runCatching { detect() }.getOrNull()
        detecting = false
    }
    val assistQuad = liveDetected ?: detectedQuad

    // Lens-style: when the page has no crop yet, drop the corners straight onto
    // the detected document as soon as detection resolves (until the user takes over).
    LaunchedEffect(assistQuad) {
        val detected = assistQuad
        if (detected != null && !userEdited && quad == Quad.FULL) {
            quad = detected
        }
    }

    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val accent = MaterialTheme.colorScheme.primary
    val detectedColor = MaterialTheme.colorScheme.tertiary

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        val image = bitmap
        if (image == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            val transform = if (viewSize.width > 0 && viewSize.height > 0) {
                OverlayTransform(
                    imageWidth = image.width.toFloat(),
                    imageHeight = image.height.toFloat(),
                    viewWidth = viewSize.width.toFloat(),
                    viewHeight = viewSize.height.toFloat(),
                    mode = OverlayTransform.ScaleMode.FIT,
                )
            } else {
                null
            }

            Canvas(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 96.dp)
                    .onSizeChanged { viewSize = it }
                    .pointerInput(image, viewSize) {
                        if (viewSize.width == 0) return@pointerInput
                        val t = OverlayTransform(
                            imageWidth = image.width.toFloat(),
                            imageHeight = image.height.toFloat(),
                            viewWidth = viewSize.width.toFloat(),
                            viewHeight = viewSize.height.toFloat(),
                            mode = OverlayTransform.ScaleMode.FIT,
                        )
                        val touchRadius = with(density) { 28.dp.toPx() }
                        detectDragGestures(
                            onDragStart = { position ->
                                val viewQuad = t.imageToView(quad)
                                activeHandle = CropGeometry.hitTest(
                                    viewQuad,
                                    Vec2(position.x, position.y),
                                    touchRadius,
                                )
                                if (activeHandle != CropGeometry.HANDLE_NONE) userEdited = true
                                snapEngaged = false
                                touchPosition = position
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                touchPosition = change.position
                                if (activeHandle == CropGeometry.HANDLE_NONE) return@detectDragGestures
                                var target = t.viewToImage(
                                    Vec2(change.position.x, change.position.y),
                                )
                                val delta = Vec2(
                                    dragAmount.x / (image.width * t.scale),
                                    dragAmount.y / (image.height * t.scale),
                                )
                                // Magnetic assist: pull a dragged corner onto the
                                // detected document edge, buzzing once on contact.
                                // Read detection state here (not the captured val) so
                                // a result that resolves after setup is still used.
                                if (CropGeometry.isCornerHandle(activeHandle)) {
                                    val reference = (liveDetected ?: detectedQuad)
                                        ?.corners?.getOrNull(activeHandle)
                                    val snapped =
                                        CropGeometry.snapToCorner(target, reference, SNAP_RADIUS)
                                    val nowSnapped = snapped !== target
                                    if (nowSnapped && !snapEngaged) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    snapEngaged = nowSnapped
                                    target = snapped
                                }
                                quad = CropGeometry.drag(quad, activeHandle, target, delta)
                            },
                            onDragEnd = {
                                activeHandle = CropGeometry.HANDLE_NONE
                                snapEngaged = false
                            },
                            onDragCancel = {
                                activeHandle = CropGeometry.HANDLE_NONE
                                snapEngaged = false
                            },
                        )
                    },
            ) {
                val t = transform ?: return@Canvas
                val imageOffset = Offset(t.offsetX, t.offsetY)
                val imageDrawSize = IntSize(
                    (image.width * t.scale).toInt().coerceAtLeast(1),
                    (image.height * t.scale).toInt().coerceAtLeast(1),
                )
                drawImage(
                    image = image,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(image.width, image.height),
                    dstOffset = IntOffset(imageOffset.x.toInt(), imageOffset.y.toInt()),
                    dstSize = imageDrawSize,
                )

                val corners = quad.corners.map {
                    val v = t.imageToView(it)
                    Offset(v.x, v.y)
                }
                val quadPath = Path().apply {
                    moveTo(corners[0].x, corners[0].y)
                    for (i in 1 until corners.size) lineTo(corners[i].x, corners[i].y)
                    close()
                }

                // Dim everything outside the crop.
                val dimPath = Path().apply {
                    fillType = PathFillType.EvenOdd
                    addRect(
                        androidx.compose.ui.geometry.Rect(
                            imageOffset.x,
                            imageOffset.y,
                            imageOffset.x + imageDrawSize.width,
                            imageOffset.y + imageDrawSize.height,
                        ),
                    )
                    addPath(quadPath)
                }
                drawPath(dimPath, color = Color.Black.copy(alpha = 0.55f))

                // Faint guide showing where the document was detected.
                val guide = assistQuad
                if (guide != null && guide != quad) {
                    val guideCorners = guide.corners.map {
                        val v = t.imageToView(it)
                        Offset(v.x, v.y)
                    }
                    val guidePath = Path().apply {
                        moveTo(guideCorners[0].x, guideCorners[0].y)
                        for (i in 1 until guideCorners.size) lineTo(guideCorners[i].x, guideCorners[i].y)
                        close()
                    }
                    drawPath(
                        guidePath,
                        color = detectedColor.copy(alpha = 0.7f),
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                }

                drawPath(quadPath, color = accent, style = Stroke(2.5.dp.toPx()))

                corners.forEach { corner ->
                    drawCircle(Color.White, radius = 9.dp.toPx(), center = corner)
                    drawCircle(accent, radius = 5.5.dp.toPx(), center = corner)
                }
                for (i in 0 until 4) {
                    val mid = quad.edgeMidpoint(i)
                    val v = t.imageToView(mid)
                    drawCircle(Color.White, radius = 5.5.dp.toPx(), center = Offset(v.x, v.y))
                }

                // Magnifier loupe while a corner is being dragged.
                if (CropGeometry.isCornerHandle(activeHandle)) {
                    drawMagnifier(
                        image = image,
                        transform = t,
                        corner = quad.corners[activeHandle],
                        touch = touchPosition,
                        accent = accent,
                    )
                }
            }
        }

        // Top bar
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Rounded.Close, stringResource(R.string.cd_close), tint = Color.White)
            }
            Text(
                text = stringResource(R.string.crop_title),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
        }

        // Bottom bar
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val detected = assistQuad
            if (detecting && detected == null) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.size(12.dp))
            } else if (detected != null) {
                CropAction(
                    icon = Icons.Rounded.CropFree,
                    label = stringResource(R.string.crop_detect),
                    onClick = {
                        quad = detected
                        userEdited = true
                    },
                )
                Spacer(Modifier.size(12.dp))
            }
            CropAction(
                icon = Icons.Rounded.FitScreen,
                label = stringResource(R.string.crop_full_page),
                onClick = {
                    quad = Quad.FULL
                    userEdited = true
                },
            )
            Spacer(Modifier.weight(1f))
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
            ) {
                IconButton(onClick = { onApply(quad) }) {
                    Icon(
                        Icons.Rounded.Check,
                        stringResource(R.string.action_done),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun CropAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        color = Color.White.copy(alpha = 0.14f),
        shape = CircleShape,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMagnifier(
    image: ImageBitmap,
    transform: OverlayTransform,
    corner: Vec2,
    touch: Offset,
    accent: Color,
) {
    val loupeRadius = 56.dp.toPx()
    val zoom = 2.4f

    // Place the loupe above the finger; flip below when near the top edge.
    val rawCenter = Offset(touch.x, touch.y - loupeRadius - 36.dp.toPx())
    val center = if (rawCenter.y < loupeRadius) {
        Offset(touch.x, touch.y + loupeRadius + 36.dp.toPx())
    } else {
        rawCenter
    }

    val srcSizePx = (loupeRadius * 2) / (transform.scale * zoom)
    val srcSide = srcSizePx.toInt().coerceAtLeast(1)
        .coerceAtMost(minOf(image.width, image.height))
    val srcCenterX = corner.x * image.width
    val srcCenterY = corner.y * image.height
    val srcLeft = (srcCenterX - srcSide / 2f).toInt()
        .coerceIn(0, (image.width - srcSide).coerceAtLeast(0))
    val srcTop = (srcCenterY - srcSide / 2f).toInt()
        .coerceIn(0, (image.height - srcSide).coerceAtLeast(0))

    val loupePath = Path().apply {
        addOval(
            androidx.compose.ui.geometry.Rect(
                center.x - loupeRadius,
                center.y - loupeRadius,
                center.x + loupeRadius,
                center.y + loupeRadius,
            ),
        )
    }
    clipPath(loupePath) {
        drawRect(Color.Black)
        drawImage(
            image = image,
            srcOffset = IntOffset(srcLeft, srcTop),
            srcSize = IntSize(srcSide, srcSide),
            dstOffset = IntOffset(
                (center.x - loupeRadius).toInt(),
                (center.y - loupeRadius).toInt(),
            ),
            dstSize = IntSize((loupeRadius * 2).toInt(), (loupeRadius * 2).toInt()),
        )
    }
    drawCircle(Color.White, radius = loupeRadius, center = center, style = Stroke(3.dp.toPx()))
    // Crosshair marking the exact corner position.
    drawLine(
        accent,
        Offset(center.x - 12.dp.toPx(), center.y),
        Offset(center.x + 12.dp.toPx(), center.y),
        strokeWidth = 2.dp.toPx(),
    )
    drawLine(
        accent,
        Offset(center.x, center.y - 12.dp.toPx()),
        Offset(center.x, center.y + 12.dp.toPx()),
        strokeWidth = 2.dp.toPx(),
    )
}

/**
 * Persists the in-progress crop [Quad] across configuration changes by round-tripping
 * through its compact string form (see [Quad.encode] / [Quad.decode]).
 */
private val QuadSaver: Saver<Quad, String> = Saver(
    save = { it.encode() },
    restore = { Quad.decode(it) ?: Quad.FULL },
)

private const val EDITOR_IMAGE_SIZE = 1600

/** Magnetic snap distance (fraction of image size) for corner-to-detected-edge assist. */
private const val SNAP_RADIUS = 0.035f
