package com.scanni.app.ui.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scanni.app.R
import com.scanni.app.di.AppGraph
import com.scanni.app.domain.model.Document
import com.scanni.app.domain.model.Folder
import com.scanni.app.domain.model.SearchHit
import com.scanni.app.ui.common.ConfirmDialog
import com.scanni.app.ui.common.EmptyState
import com.scanni.app.ui.common.PageImage
import com.scanni.app.ui.common.SearchField
import com.scanni.app.ui.common.TextInputDialog
import com.scanni.app.ui.common.formatTimestamp
import com.scanni.app.ui.common.graphViewModel
import com.scanni.app.ui.common.parseSnippet
import com.scanni.app.ui.common.rememberDateFormatter

@Composable
fun LibraryScreen(
    graph: AppGraph,
    onScan: () -> Unit,
    onOpenDocument: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val viewModel = graphViewModel { LibraryViewModel(graph.documentRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showNewFolderDialog by rememberSaveable { mutableStateOf(false) }
    var renameFolderTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteFolderTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var showMoveSheet by rememberSaveable { mutableStateOf(false) }
    var showDeleteDocsDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LibraryHeader(
                state = state,
                onQueryChange = viewModel::setQuery,
                onSelectFolder = viewModel::selectFolder,
                onNewFolder = { showNewFolderDialog = true },
                onRenameFolder = { renameFolderTarget = it },
                onDeleteFolder = { deleteFolderTarget = it },
                onClearSelection = viewModel::clearSelection,
                onDeleteSelected = { showDeleteDocsDialog = true },
                onMoveSelected = { showMoveSheet = true },
                onOpenSettings = onOpenSettings,
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !state.inSelectionMode,
                enter = scaleIn(tween(180)) + fadeIn(tween(180)),
                exit = scaleOut(tween(140)) + fadeOut(tween(140)),
            ) {
                ExtendedFloatingActionButton(
                    onClick = onScan,
                    icon = {
                        Icon(Icons.Outlined.DocumentScanner, contentDescription = null)
                    },
                    text = { Text(stringResource(R.string.library_scan)) },
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when {
                state.isSearching -> SearchResults(
                    hits = state.searchHits,
                    query = state.query,
                    onOpenDocument = onOpenDocument,
                )

                state.documents.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.DocumentScanner,
                    title = stringResource(R.string.library_empty_title),
                    body = stringResource(R.string.library_empty_body),
                )

                else -> DocumentGrid(
                    documents = state.documents,
                    selection = state.selection,
                    selectionMode = state.inSelectionMode,
                    onOpen = onOpenDocument,
                    onToggleSelection = viewModel::toggleSelection,
                )
            }
        }
    }

    if (showNewFolderDialog) {
        TextInputDialog(
            title = stringResource(R.string.folder_new),
            placeholder = stringResource(R.string.folder_name_hint),
            confirmLabel = stringResource(R.string.action_create),
            onConfirm = {
                viewModel.createFolder(it)
                showNewFolderDialog = false
            },
            onDismiss = { showNewFolderDialog = false },
        )
    }

    renameFolderTarget?.let { folderId ->
        val current = state.folders.firstOrNull { it.id == folderId }
        TextInputDialog(
            title = stringResource(R.string.action_rename),
            placeholder = stringResource(R.string.folder_name_hint),
            confirmLabel = stringResource(R.string.action_rename),
            initialValue = current?.name.orEmpty(),
            onConfirm = {
                viewModel.renameFolder(folderId, it)
                renameFolderTarget = null
            },
            onDismiss = { renameFolderTarget = null },
        )
    }

    deleteFolderTarget?.let { folderId ->
        val folder = state.folders.firstOrNull { it.id == folderId }
        ConfirmDialog(
            message = stringResource(R.string.folder_delete_message, folder?.name.orEmpty()),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                viewModel.deleteFolder(folderId)
                deleteFolderTarget = null
            },
            onDismiss = { deleteFolderTarget = null },
        )
    }

    if (showDeleteDocsDialog) {
        ConfirmDialog(
            message = pluralStringResource(
                R.plurals.delete_documents_message,
                state.selection.size,
                state.selection.size,
            ),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                viewModel.deleteSelected()
                showDeleteDocsDialog = false
            },
            onDismiss = { showDeleteDocsDialog = false },
        )
    }

    if (showMoveSheet) {
        MoveToFolderSheet(
            folders = state.folders,
            onMove = { folderId ->
                viewModel.moveSelected(folderId)
                showMoveSheet = false
            },
            onDismiss = { showMoveSheet = false },
        )
    }
}

@Composable
private fun LibraryHeader(
    state: LibraryUiState,
    onQueryChange: (String) -> Unit,
    onSelectFolder: (String?) -> Unit,
    onNewFolder: () -> Unit,
    onRenameFolder: (String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onMoveSelected: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        AnimatedContent(
            targetState = state.inSelectionMode,
            label = "libraryHeader",
            modifier = Modifier.fillMaxWidth(),
        ) { inSelection ->
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
        ) {
            if (inSelection) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onClearSelection) {
                        Icon(Icons.Outlined.Close, stringResource(R.string.cd_close))
                    }
                    Text(
                        text = stringResource(R.string.library_selected_count, state.selection.size),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onMoveSelected) {
                        Icon(
                            Icons.AutoMirrored.Outlined.DriveFileMove,
                            stringResource(R.string.folder_move_title),
                        )
                    }
                    IconButton(onClick = onDeleteSelected) {
                        Icon(Icons.Outlined.Delete, stringResource(R.string.action_delete))
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .padding(start = 20.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    FilledTonalIconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, stringResource(R.string.library_settings))
                    }
                }
                SearchField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    hint = stringResource(R.string.library_search_hint),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
                if (!state.isSearching) {
                    FolderBar(
                        folders = state.folders,
                        selectedFolderId = state.selectedFolderId,
                        onSelect = onSelectFolder,
                        onNewFolder = onNewFolder,
                        onRename = onRenameFolder,
                        onDelete = onDeleteFolder,
                    )
                } else {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        }
    }
}

@Composable
private fun FolderBar(
    folders: List<Folder>,
    selectedFolderId: String?,
    onSelect: (String?) -> Unit,
    onNewFolder: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "all") {
            FilterChip(
                selected = selectedFolderId == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(R.string.folder_all_documents)) },
            )
        }
        items(folders.size, key = { folders[it].id }) { index ->
            val folder = folders[index]
            var menuOpen by remember { mutableStateOf(false) }
            Box {
                FilterChip(
                    selected = selectedFolderId == folder.id,
                    // Tapping the already-selected folder opens its management menu.
                    onClick = {
                        if (selectedFolderId == folder.id) menuOpen = true else onSelect(folder.id)
                    },
                    label = { Text("${folder.name} · ${folder.documentCount}") },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_rename)) },
                        onClick = {
                            menuOpen = false
                            onRename(folder.id)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete)) },
                        onClick = {
                            menuOpen = false
                            onDelete(folder.id)
                        },
                    )
                }
            }
        }
        item(key = "new") {
            FilterChip(
                selected = false,
                onClick = onNewFolder,
                label = { Text(stringResource(R.string.folder_new)) },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DocumentGrid(
    documents: List<Document>,
    selection: Set<String>,
    selectionMode: Boolean,
    onOpen: (String) -> Unit,
    onToggleSelection: (String) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val dateFormatter = rememberDateFormatter()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 156.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(documents, key = { it.id }) { document ->
            val selected = document.id in selection
            ElevatedCard(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (selected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                ),
                elevation = CardDefaults.elevatedCardElevation(
                    defaultElevation = if (selected) 0.dp else 2.dp,
                ),
                modifier = Modifier
                    .animateItem()
                    .combinedClickable(
                        onClick = {
                            if (selectionMode) onToggleSelection(document.id) else onOpen(document.id)
                        },
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleSelection(document.id)
                        },
                    ),
            ) {
                Box(Modifier.padding(6.dp)) {
                    PageImage(
                        path = document.thumbPath,
                        revision = document.updatedAt.hashCode(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f)
                            .clip(MaterialTheme.shapes.medium),
                    )
                    if (selected) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(26.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape),
                        )
                    }
                }
                Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 12.dp)) {
                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = formatTimestamp(document.createdAt, dateFormatter) + " · " +
                            pluralStringResource(
                                R.plurals.library_page_count,
                                document.pageCount,
                                document.pageCount,
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResults(
    hits: List<SearchHit>,
    query: String,
    onOpenDocument: (String) -> Unit,
) {
    if (hits.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.Search,
            title = stringResource(R.string.library_no_results, query),
            body = "",
        )
        return
    }
    val dateFormatter = rememberDateFormatter()
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 8.dp),
    ) {
        items(hits.size, key = { hits[it].document.id }) { index ->
            val hit = hits[index]
            ListItem(
                headlineContent = {
                    Text(hit.document.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                supportingContent = {
                    val snippet = hit.snippet
                    if (snippet != null) {
                        Text(
                            parseSnippet(snippet),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else {
                        Text(formatTimestamp(hit.document.createdAt, dateFormatter))
                    }
                },
                leadingContent = {
                    PageImage(
                        path = hit.document.thumbPath,
                        revision = hit.document.updatedAt.hashCode(),
                        modifier = Modifier
                            .width(48.dp)
                            .aspectRatio(3f / 4f)
                            .clip(MaterialTheme.shapes.small),
                    )
                },
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onOpenDocument(hit.document.id) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveToFolderSheet(
    folders: List<Folder>,
    onMove: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.folder_move_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.folder_none)) },
                    leadingContent = { Icon(Icons.Outlined.Close, contentDescription = null) },
                    modifier = Modifier.clickable { onMove(null) },
                )
            }
            items(folders.size, key = { folders[it].id }) { index ->
                val folder = folders[index]
                ListItem(
                    headlineContent = { Text(folder.name) },
                    supportingContent = { Text(folder.documentCount.toString()) },
                    leadingContent = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                    modifier = Modifier.clickable { onMove(folder.id) },
                )
            }
        }
    }
}
