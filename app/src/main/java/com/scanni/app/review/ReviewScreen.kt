package com.scanni.app.review

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import com.scanni.app.processing.EnhancementMode
import kotlin.math.roundToInt

@Composable
fun ReviewScreen(
    state: ReviewUiState,
    onPageSelected: (Int) -> Unit,
    onModeChange: (EnhancementMode) -> Unit,
    onCropChanged: (List<Float>) -> Unit,
    onCropChangeFinished: () -> Unit,
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
            itemsIndexed(state.pages) { index, _ ->
                Button(
                    modifier = Modifier.testTag("review-page-button-$index"),
                    onClick = { onPageSelected(index) }
                ) {
                    Text(text = "Page ${index + 1}")
                }
            }
        }

        if (activePage != null) {
            ReviewCropEditor(
                page = activePage,
                onCropChanged = onCropChanged,
                onCropChangeFinished = onCropChangeFinished
            )
            Text(text = "Mode: ${activePage.mode.name}")
            Text(text = "Original: ${activePage.originalPath}")
            Text(text = "Processed: ${activePage.processedPath}")
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
                    for (index in points.indices) {
                        val nextPoint = points[(index + 1) % points.size]
                        drawLine(
                            color = Color(0xFF1B4D3E),
                            start = points[index],
                            end = nextPoint,
                            strokeWidth = 6f
                        )
                    }
                }
            }

            repeat(page.corners.size / 2) { index ->
                CropHandle(
                    modifier = Modifier.align(Alignment.TopStart),
                    index = index,
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
