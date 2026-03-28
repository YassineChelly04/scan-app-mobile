package com.scanni.app.review

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.scanni.app.processing.EnhancementMode
import kotlin.math.roundToInt

@Composable
fun ReviewScreen(
    state: ReviewUiState,
    onPageSelected: (Int) -> Unit,
    onModeChange: (EnhancementMode) -> Unit,
    onCropChanged: (List<Float>) -> Unit,
    onCropChangeFinished: () -> Unit,
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onDeletePage: () -> Unit,
    onMovePage: (Int, Int) -> Unit,
    onAddAnotherPageClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    val activePage = state.activePage
    val saveEnabled = state.pages.isNotEmpty() && state.pages.all { page ->
        page.processedPath.isNotBlank() && !page.isProcessing
    }

    Column(
        modifier = Modifier
            .testTag("review-screen")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("review-page-switcher"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(state.pages) { index, page ->
                ReviewPageStripCard(
                    index = index,
                    page = page,
                    isActive = index == state.activePageIndex,
                    maxIndex = state.pages.lastIndex,
                    onPageSelected = onPageSelected,
                    onMovePage = onMovePage
                )
            }
        }

        if (activePage != null) {
            ReviewCropEditor(
                page = activePage,
                onCropChanged = onCropChanged,
                onCropChangeFinished = onCropChangeFinished
            )
            Text(text = "Mode: ${activePage.mode.name}")
            Text(text = "Rotation: ${activePage.rotationQuarterTurns * 90}°")
            Text(text = "Original: ${activePage.originalPath}")
            Text(text = "Processed: ${activePage.processedPath}")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    modifier = Modifier.testTag("review-rotate-left"),
                    onClick = onRotateLeft
                ) {
                    Text(text = "Rotate Left")
                }
                Button(
                    modifier = Modifier.testTag("review-rotate-right"),
                    onClick = onRotateRight
                ) {
                    Text(text = "Rotate Right")
                }
                Button(
                    modifier = Modifier.testTag("review-delete-page"),
                    onClick = onDeletePage,
                    enabled = state.pages.size > 1
                ) {
                    Text(text = "Delete")
                }
            }
        } else {
            Text(text = "No pages loaded")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onModeChange(EnhancementMode.DOCUMENT) }) {
                Text(text = "Document")
            }
            Button(onClick = { onModeChange(EnhancementMode.BOOK) }) {
                Text(text = "Book")
            }
            Button(onClick = { onModeChange(EnhancementMode.WHITEBOARD) }) {
                Text(text = "Whiteboard")
            }
        }

        Button(onClick = onAddAnotherPageClick) {
            Text(text = "Add Another Page")
        }
        Button(
            onClick = onSaveClick,
            enabled = saveEnabled
        ) {
            Text(text = "Save Document")
        }

        activePage?.errorMessage?.let { errorMessage ->
            Text(text = "Error: $errorMessage")
        }
        if (activePage?.isProcessing == true) {
            Text(text = "Processing...")
        }
    }
}

@Composable
private fun ReviewPageStripCard(
    index: Int,
    page: ReviewPageState,
    isActive: Boolean,
    maxIndex: Int,
    onPageSelected: (Int) -> Unit,
    onMovePage: (Int, Int) -> Unit
) {
    val density = LocalDensity.current
    val dragSlotWidthPx = with(density) { 96.dp.toPx() }
    var totalDragX = 0f

    Card(
        modifier = Modifier
            .size(width = 88.dp, height = 72.dp)
            .testTag("review-page-card-$index")
            .clickable { onPageSelected(index) }
            .pointerInput(index, maxIndex) {
                detectDragGestures(
                    onDragStart = { totalDragX = 0f },
                    onDragEnd = {
                        val shift = (totalDragX / dragSlotWidthPx).roundToInt()
                        val nextIndex = (index + shift).coerceIn(0, maxIndex)
                        if (nextIndex != index) {
                            onMovePage(index, nextIndex)
                        }
                    }
                ) { change, dragAmount ->
                    change.consume()
                    totalDragX += dragAmount.x
                }
            }
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) Color(0xFF1B4D3E) else Color(0xFFB8AA93),
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFFE6F0EB) else Color(0xFFF4EDE3)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Page ${index + 1}", modifier = Modifier.testTag("review-page-button-$index"))
            Text(text = "${page.rotationQuarterTurns * 90}°")
        }
    }
}

@Composable
private fun ReviewCropEditor(
    page: ReviewPageState,
    onCropChanged: (List<Float>) -> Unit,
    onCropChangeFinished: () -> Unit
) {
    val imageBitmap = remember(page.originalPath) {
        BitmapFactory.decodeFile(page.originalPath)?.asImageBitmap()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .border(width = 1.dp, color = Color(0xFFB8AA93))
            .background(Color(0xFFF4EDE3))
            .testTag("review-crop-editor")
    ) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val handleSize = 24.dp

        Box(modifier = Modifier.fillMaxSize()) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Review page",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("crop-overlay")
            ) {
                val points = page.corners.asOffsets(size.width, size.height)
                if (points.size == 4) {
                    for (drawIndex in points.indices) {
                        val nextPoint = points[(drawIndex + 1) % points.size]
                        drawLine(
                            color = Color(0xFF1B4D3E),
                            start = points[drawIndex],
                            end = nextPoint,
                            strokeWidth = 6f
                        )
                    }
                }
            }

            repeat(page.corners.size / 2) { handleIndex ->
                CropHandle(
                    modifier = Modifier.align(Alignment.TopStart),
                    index = handleIndex,
                    corners = page.corners,
                    widthPx = widthPx,
                    heightPx = heightPx,
                    handleSize = handleSize,
                    onCropChanged = onCropChanged,
                    onCropChangeFinished = onCropChangeFinished
                )
            }
        }
    }
}

@Composable
private fun CropHandle(
    modifier: Modifier,
    index: Int,
    corners: List<Float>,
    widthPx: Float,
    heightPx: Float,
    handleSize: Dp,
    onCropChanged: (List<Float>) -> Unit,
    onCropChangeFinished: () -> Unit
) {
    val density = LocalDensity.current
    val handleSizePx = with(density) { handleSize.toPx() }
    val x = corners[index * 2].coerceIn(0f, 1f) * widthPx
    val y = corners[index * 2 + 1].coerceIn(0f, 1f) * heightPx

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    x = (x - handleSizePx / 2f).roundToInt(),
                    y = (y - handleSizePx / 2f).roundToInt()
                )
            }
            .size(handleSize)
            .clip(CircleShape)
            .background(Color.White)
            .border(width = 2.dp, color = Color(0xFF1B4D3E), shape = CircleShape)
            .testTag("crop-handle-$index")
            .pointerInput(corners, widthPx, heightPx) {
                detectDragGestures(
                    onDragEnd = onCropChangeFinished
                ) { change, dragAmount ->
                    change.consume()
                    val updatedCorners = corners.toMutableList()
                    updatedCorners[index * 2] =
                        (updatedCorners[index * 2] + (dragAmount.x / widthPx)).coerceIn(0f, 1f)
                    updatedCorners[index * 2 + 1] =
                        (updatedCorners[index * 2 + 1] + (dragAmount.y / heightPx)).coerceIn(0f, 1f)
                    onCropChanged(updatedCorners)
                }
            }
    )
}

private fun List<Float>.asOffsets(width: Float, height: Float): List<Offset> =
    chunked(2).map { pair ->
        Offset(
            x = pair[0].coerceIn(0f, 1f) * width,
            y = pair[1].coerceIn(0f, 1f) * height
        )
    }
