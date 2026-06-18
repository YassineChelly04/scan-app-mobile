package com.scanni.app.ui.document

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scanni.app.R
import com.scanni.app.di.AppGraph
import com.scanni.app.domain.model.OcrStatus
import com.scanni.app.domain.model.Page
import com.scanni.app.export.ShareActions
import com.scanni.app.ui.common.ConfirmDialog
import com.scanni.app.ui.common.EventEffect
import com.scanni.app.ui.common.TextInputDialog
import com.scanni.app.ui.common.ZoomableImage
import com.scanni.app.ui.common.graphViewModel
import com.scanni.app.ui.library.MoveToFolderSheet

@Composable
fun DocumentScreen(
    graph: AppGraph,
    documentId: String,
    onBack: () -> Unit,
    onEditPage: (String) -> Unit,
) {
    val context = LocalContext.current
    val viewModel = graphViewModel(key = documentId) {
        DocumentViewModel(
            documentId = documentId,
            repository = graph.documentRepository,
            exportDocument = graph.exportDocumentUseCase,
            ocrScheduler = graph.ocrScheduler,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }
    var showExportSheet by rememberSaveable { mutableStateOf(false) }
    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    var showMoveSheet by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    val copiedMessage = stringResource(R.string.document_text_copied)
    val exportFailedMessage = stringResource(R.string.document_export_failed)
    val clipboardLabel = stringResource(R.string.app_name)

    val savePdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        if (uri != null) {
            viewModel.savePdfTo(uri) { file, target ->
                ShareActions.copyToUri(context, file, target)
            }
        }
    }

    EventEffect(viewModel.events) { event ->
        when (event) {
            is DocumentEvent.SharePdf -> ShareActions.sharePdf(context, event.file)
            is DocumentEvent.ShareImages -> ShareActions.shareImages(context, event.files)
            is DocumentEvent.CopyText -> {
                ShareActions.copyToClipboard(context, clipboardLabel, event.text)
                Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
            }
            DocumentEvent.Deleted -> onBack()
            DocumentEvent.ExportFailed ->
                Toast.makeText(context, exportFailedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    // Document deleted elsewhere -> leave.
    LaunchedEffect(state.loaded, state.document) {
        if (state.loaded && state.document == null) onBack()
    }

    val document = state.document

    Scaffold(
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.cd_back))
                }
                Text(
                    text = document?.title.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showRenameDialog = true },
                )
                FilledTonalButton(
                    onClick = { showExportSheet = true },
                    enabled = !state.exporting && state.pages.isNotEmpty(),
                ) {
                    if (state.exporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            Icons.Rounded.IosShare,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.document_export))
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Rounded.MoreVert, stringResource(R.string.cd_more_options))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_rename)) },
                            leadingIcon = {
                                Icon(Icons.Rounded.DriveFileRenameOutline, null)
                            },
                            onClick = {
                                menuOpen = false
                                showRenameDialog = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.folder_move_title)) },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Rounded.DriveFileMove, null)
                            },
                            onClick = {
                                menuOpen = false
                                showMoveSheet = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.document_text_rerun)) },
                            leadingIcon = { Icon(Icons.Rounded.Refresh, null) },
                            onClick = {
                                menuOpen = false
                                viewModel.rerunOcr()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_delete)) },
                            leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                            onClick = {
                                menuOpen = false
                                showDeleteDialog = true
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TabRow(selectedTabIndex = tab) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = { Text(stringResource(R.string.document_tab_pages)) },
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = { Text(stringResource(R.string.document_tab_text)) },
                )
            }
            Crossfade(targetState = tab, label = "documentTabs") { currentTab ->
                when (currentTab) {
                    0 -> PagesTab(pages = state.pages, onEditPage = onEditPage)
                    else -> TextTab(
                        pages = state.pages,
                        onCopyAll = viewModel::copyAllText,
                        onRerun = viewModel::rerunOcr,
                    )
                }
            }
        }
    }

    if (showExportSheet) {
        ExportSheet(
            onSharePdf = {
                showExportSheet = false
                viewModel.sharePdf()
            },
            onSavePdf = {
                showExportSheet = false
                savePdfLauncher.launch("${document?.title ?: "Scanni"}.pdf")
            },
            onShareImages = {
                showExportSheet = false
                viewModel.shareImages()
            },
            onCopyText = {
                showExportSheet = false
                viewModel.copyAllText()
            },
            onDismiss = { showExportSheet = false },
        )
    }

    if (showRenameDialog && document != null) {
        TextInputDialog(
            title = stringResource(R.string.document_rename_title),
            placeholder = stringResource(R.string.review_document_name_hint),
            confirmLabel = stringResource(R.string.action_rename),
            initialValue = document.title,
            onConfirm = {
                viewModel.rename(it)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false },
        )
    }

    if (showMoveSheet) {
        MoveToFolderSheet(
            folders = state.folders,
            onMove = {
                viewModel.moveTo(it)
                showMoveSheet = false
            },
            onDismiss = { showMoveSheet = false },
        )
    }

    if (showDeleteDialog && document != null) {
        ConfirmDialog(
            message = stringResource(R.string.document_delete_message, document.title),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                showDeleteDialog = false
                viewModel.delete()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

@Composable
private fun PagesTab(
    pages: List<Page>,
    onEditPage: (String) -> Unit,
) {
    if (pages.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            pageSpacing = 16.dp,
        ) { index ->
            val page = pages[index]
            val revision = remember(page.quad, page.rotationDeg, page.filter) {
                pageRevision(page)
            }
            Box(Modifier.fillMaxSize()) {
                ZoomableImage(
                    path = page.processedPath,
                    revision = revision,
                    modifier = Modifier.fillMaxSize(),
                )
                SmallFloatingActionButton(
                    onClick = { onEditPage(page.id) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp),
                ) {
                    Icon(Icons.Rounded.Edit, stringResource(R.string.document_edit_page))
                }
            }
        }
        Text(
            text = stringResource(
                R.string.review_page_indicator,
                pagerState.currentPage + 1,
                pages.size,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 10.dp),
        )
    }
}

@Composable
private fun TextTab(
    pages: List<Page>,
    onCopyAll: () -> Unit,
    onRerun: () -> Unit,
) {
    val anyRunning = pages.any {
        it.ocrStatus == OcrStatus.PENDING || it.ocrStatus == OcrStatus.RUNNING
    }
    val anyFailed = pages.any { it.ocrStatus == OcrStatus.FAILED }
    val hasText = pages.any { !it.ocrText.isNullOrBlank() }

    Column(Modifier.fillMaxSize()) {
        if (anyRunning) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(
                    stringResource(R.string.document_text_pending),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
        if (anyFailed && !anyRunning) {
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.document_text_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onRerun) {
                    Text(stringResource(R.string.action_retry))
                }
            }
        }
        if (hasText) {
            Row(Modifier.padding(horizontal = 12.dp)) {
                TextButton(onClick = onCopyAll) {
                    Icon(
                        Icons.Rounded.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.document_export_text))
                }
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(pages.size, key = { pages[it].id }) { index ->
                val page = pages[index]
                Column(Modifier.padding(bottom = 20.dp)) {
                    Text(
                        text = stringResource(
                            R.string.review_page_indicator,
                            index + 1,
                            pages.size,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(6.dp))
                    when {
                        !page.ocrText.isNullOrBlank() -> SelectionContainer {
                            Text(
                                text = page.ocrText,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }

                        page.ocrStatus == OcrStatus.DONE -> Text(
                            text = stringResource(R.string.document_text_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        else -> Text(
                            text = "…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportSheet(
    onSharePdf: () -> Unit,
    onSavePdf: () -> Unit,
    onShareImages: () -> Unit,
    onCopyText: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.document_export),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.document_export_pdf)) },
            supportingContent = { Text(stringResource(R.string.document_export_pdf_desc)) },
            leadingContent = { Icon(Icons.Rounded.PictureAsPdf, null) },
            modifier = Modifier.clickable(onClick = onSharePdf),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.document_export_save_pdf)) },
            leadingContent = { Icon(Icons.Rounded.SaveAlt, null) },
            modifier = Modifier.clickable(onClick = onSavePdf),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.document_export_images)) },
            leadingContent = { Icon(Icons.Rounded.Image, null) },
            modifier = Modifier.clickable(onClick = onShareImages),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.document_export_text)) },
            leadingContent = { Icon(Icons.Rounded.ContentCopy, null) },
            modifier = Modifier
                .clickable(onClick = onCopyText)
                .padding(bottom = 24.dp),
        )
        Spacer(Modifier.height(16.dp))
    }
}

internal fun pageRevision(page: Page): Int =
    (page.quad?.encode().orEmpty() + page.rotationDeg + page.filter.name).hashCode()
