package com.scanni.app.ui.review

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Rotate90DegreesCw
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scanni.app.R
import com.scanni.app.di.AppGraph
import com.scanni.app.domain.model.CapturedPage
import com.scanni.app.domain.model.ScanFilter
import com.scanni.app.ui.common.ConfirmDialog
import com.scanni.app.ui.common.FilterRow
import com.scanni.app.ui.common.CropEditor
import com.scanni.app.ui.common.PageImage
import com.scanni.app.ui.common.TextInputDialog
import com.scanni.app.ui.common.graphViewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun ReviewScreen(
    graph: AppGraph,
    onBackToCamera: () -> Unit,
    onDiscarded: () -> Unit,
    onSaved: (String) -> Unit,
) {
    val context = LocalContext.current
    val viewModel = graphViewModel {
        ReviewViewModel(
            session = graph.scanSession,
            fileStore = graph.fileStore,
            processor = graph.pageProcessor,
            saveScan = graph.saveScanUseCase,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var showDeletePageDialog by rememberSaveable { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { state.pages.size })
    val currentPage = state.pages.getOrNull(pagerState.currentPage)

    val saveFailedMessage = stringResource(R.string.document_export_failed)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ReviewEvent.Saved -> onSaved(event.documentId)
                ReviewEvent.SaveFailed ->
                    Toast.makeText(context, saveFailedMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // All pages deleted -> back to the camera.
    LaunchedEffect(state.pages.isEmpty()) {
        if (state.pages.isEmpty() && !state.saving) onBackToCamera()
    }

    LaunchedEffect(pagerState.currentPage, state.pages.size) {
        currentPage?.let { viewModel.ensureFilterChips(it.id) }
    }

    BackHandler(enabled = state.croppingPageId == null) { showDiscardDialog = true }

    Scaffold(
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBackToCamera) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.cd_back))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.review_title),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (state.pages.isNotEmpty()) {
                        Text(
                            stringResource(
                                R.string.review_page_indicator,
                                pagerState.currentPage + 1,
                                state.pages.size,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(
                    onClick = { showDeletePageDialog = true },
                    enabled = currentPage != null && !state.saving,
                ) {
                    Icon(Icons.Outlined.Delete, stringResource(R.string.action_delete))
                }
                Button(
                    onClick = { showSaveDialog = true },
                    enabled = state.pages.isNotEmpty() && !state.saving,
                    modifier = Modifier.padding(end = 12.dp),
                ) {
                    Text(
                        if (state.saving) {
                            stringResource(R.string.review_saving)
                        } else {
                            stringResource(R.string.action_save)
                        },
                    )
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Page preview pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                pageSpacing = 16.dp,
                contentPadding = PaddingValues(horizontal = 24.dp),
            ) { index ->
                val page = state.pages.getOrNull(index) ?: return@HorizontalPager
                val preview = state.previews[page.id]
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    PageImage(
                        path = preview?.path,
                        revision = preview?.key?.hashCode() ?: 0,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.medium),
                    )
                    if (preview == null || preview.processing) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Filter carousel for the current page
            currentPage?.let { page ->
                FilterRow(
                    selected = page.filter,
                    chipPathFor = { filter -> state.filterChips["${page.id}:${filter.name}"] },
                    onSelect = { viewModel.setFilter(page.id, it) },
                )
            }

            // Toolbar: crop / rotate / add page
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ToolbarAction(
                    icon = Icons.Outlined.Crop,
                    label = stringResource(R.string.review_action_crop),
                    enabled = currentPage != null,
                ) { currentPage?.let { viewModel.openCrop(it.id) } }
                ToolbarAction(
                    icon = Icons.Outlined.Rotate90DegreesCw,
                    label = stringResource(R.string.review_action_rotate),
                    enabled = currentPage != null,
                ) { currentPage?.let { viewModel.rotate(it.id) } }
                ToolbarAction(
                    icon = Icons.Outlined.Add,
                    label = stringResource(R.string.review_action_add_page),
                    enabled = !state.saving,
                ) { onBackToCamera() }
            }

            // Reorderable thumbnail strip
            ThumbnailStrip(
                pages = state.pages,
                previews = state.previews,
                currentIndex = pagerState.currentPage,
                onSelect = { scope.launch { pagerState.animateScrollToPage(it) } },
                onMove = { from, to ->
                    viewModel.movePage(from, to)
                    scope.launch { pagerState.scrollToPage(to) }
                },
            )
        }
    }

    // --- Overlays & dialogs ---

    state.croppingPageId?.let { pageId ->
        val page = state.pages.firstOrNull { it.id == pageId }
        if (page != null) {
            Box(Modifier.zIndex(10f)) {
                CropEditor(
                    imagePath = page.originalPath,
                    initialQuad = page.quad,
                    detectedQuad = page.detectedQuad,
                    onApply = { viewModel.applyCrop(pageId, it) },
                    onCancel = { viewModel.closeCrop() },
                )
            }
        }
    }

    if (showSaveDialog) {
        val prefix = stringResource(R.string.review_default_name_prefix)
        TextInputDialog(
            title = stringResource(R.string.review_save_dialog_title),
            placeholder = stringResource(R.string.review_document_name_hint),
            confirmLabel = stringResource(R.string.action_save),
            initialValue = remember { graph.documentNamer.defaultName(prefix) },
            onConfirm = { title ->
                showSaveDialog = false
                viewModel.save(title)
            },
            onDismiss = { showSaveDialog = false },
        )
    }

    if (showDiscardDialog) {
        ConfirmDialog(
            title = stringResource(R.string.review_discard_title),
            message = stringResource(R.string.review_discard_body),
            confirmLabel = stringResource(R.string.review_discard_confirm),
            onConfirm = {
                showDiscardDialog = false
                viewModel.discard()
                onDiscarded()
            },
            onDismiss = { showDiscardDialog = false },
        )
    }

    if (showDeletePageDialog && currentPage != null) {
        ConfirmDialog(
            message = stringResource(R.string.review_delete_page_message),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                showDeletePageDialog = false
                viewModel.deletePage(currentPage.id)
            },
            onDismiss = { showDeletePageDialog = false },
        )
    }
}

@Composable
private fun ToolbarAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            },
        )
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

/** Thumbnail strip with long-press drag to reorder. */
@Composable
private fun ThumbnailStrip(
    pages: List<CapturedPage>,
    previews: Map<String, PagePreview>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    val density = LocalDensity.current
    val itemSlotPx = with(density) { (56 + 8).dp.toPx() }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(pages.size, key = { pages[it].id }) { index ->
            val page = pages[index]
            val isDragging = index == draggingIndex
            Box(
                Modifier
                    .size(56.dp)
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        if (isDragging) {
                            translationX = dragOffsetX
                            scaleX = 1.08f
                            scaleY = 1.08f
                        }
                    }
                    .clip(MaterialTheme.shapes.small)
                    .border(
                        width = if (index == currentIndex) 2.5.dp else 1.dp,
                        color = if (index == currentIndex) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = MaterialTheme.shapes.small,
                    )
                    .clickable { onSelect(index) }
                    .pointerInput(index, pages.size) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingIndex = index
                                dragOffsetX = 0f
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffsetX += amount.x
                            },
                            onDragEnd = {
                                val shift = (dragOffsetX / itemSlotPx).roundToInt()
                                val target = (index + shift).coerceIn(0, pages.size - 1)
                                if (target != index) onMove(index, target)
                                draggingIndex = -1
                                dragOffsetX = 0f
                            },
                            onDragCancel = {
                                draggingIndex = -1
                                dragOffsetX = 0f
                            },
                        )
                    },
            ) {
                PageImage(
                    path = previews[page.id]?.path,
                    revision = previews[page.id]?.key?.hashCode() ?: 0,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
