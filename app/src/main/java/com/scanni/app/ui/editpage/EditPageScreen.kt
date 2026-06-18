package com.scanni.app.ui.editpage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Rotate90DegreesCw
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.clickable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scanni.app.R
import com.scanni.app.di.AppGraph
import com.scanni.app.ui.common.CropEditor
import com.scanni.app.ui.common.EventEffect
import com.scanni.app.ui.common.FilterRow
import com.scanni.app.ui.common.PageImage
import com.scanni.app.ui.common.graphViewModel
import com.scanni.app.core.geometry.Quad

@Composable
fun EditPageScreen(
    graph: AppGraph,
    pageId: String,
    onDone: () -> Unit,
) {
    val viewModel = graphViewModel(key = pageId) {
        EditPageViewModel(
            pageId = pageId,
            repository = graph.documentRepository,
            fileStore = graph.fileStore,
            processor = graph.pageProcessor,
            updatePage = graph.updatePageUseCase,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    EventEffect(viewModel.events) { event ->
        when (event) {
            EditPageEvent.Saved -> onDone()
            EditPageEvent.NotFound -> onDone()
        }
    }

    Scaffold(
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDone) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.cd_back))
                }
                Text(
                    text = stringResource(R.string.edit_page_title),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { viewModel.save() },
                    enabled = state.page != null && !state.saving,
                    modifier = Modifier.padding(end = 12.dp),
                ) {
                    Text(
                        if (state.saving) {
                            stringResource(R.string.processing)
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
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                PageImage(
                    path = state.previewPath,
                    revision = state.previewKey.hashCode(),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.medium),
                )
                if (state.processing) {
                    CircularProgressIndicator()
                }
            }

            FilterRow(
                selected = state.filter,
                chipPathFor = { state.filterChips[it.name] },
                onSelect = viewModel::setFilter,
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                EditAction(
                    icon = Icons.Rounded.Crop,
                    label = stringResource(R.string.review_action_crop),
                    onClick = viewModel::openCrop,
                )
                EditAction(
                    icon = Icons.Rounded.Rotate90DegreesCw,
                    label = stringResource(R.string.review_action_rotate),
                    onClick = viewModel::rotate,
                )
            }
        }
    }

    if (state.cropping) {
        val page = state.page
        if (page != null) {
            Box(Modifier.zIndex(10f)) {
                CropEditor(
                    imagePath = page.originalPath,
                    initialQuad = state.quad ?: Quad.FULL,
                    detectedQuad = null,
                    onApply = viewModel::applyCrop,
                    onCancel = viewModel::closeCrop,
                    detect = { graph.documentDetector.detectFile(page.originalPath) },
                )
            }
        }
    }
}

@Composable
private fun EditAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Icon(icon, contentDescription = null)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
