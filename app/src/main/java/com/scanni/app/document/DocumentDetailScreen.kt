package com.scanni.app.document

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.io.File

data class DocumentFolderOption(
    val id: Long,
    val name: String
)

data class DocumentDetailUiState(
    val title: String = "",
    val editableTitle: String = "",
    val pageCount: Int = 0,
    val ocrStatus: String = "",
    val selectedFolderId: Long? = null,
    val availableFolders: List<DocumentFolderOption> = emptyList(),
    val isLoading: Boolean = true,
    val isSavingMetadata: Boolean = false,
    val isExporting: Boolean = false,
    val canSaveMetadata: Boolean = false,
    val canShare: Boolean = false,
    val errorMessage: String? = null,
    val generatedPdf: File? = null
)

@Composable
fun DocumentDetailScreen(
    state: DocumentDetailUiState,
    onTitleChange: (String) -> Unit = {},
    onFolderSelected: (Long?) -> Unit = {},
    onSaveDetailsClick: () -> Unit = {},
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("document-detail-screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.isLoading) {
            Text(text = "Loading document...")
        }

        Text(
            text = state.title,
            style = MaterialTheme.typography.headlineSmall
        )
        Text(text = "Pages: ${state.pageCount}")
        Text(text = "OCR: ${state.ocrStatus}")

        OutlinedTextField(
            value = state.editableTitle,
            onValueChange = onTitleChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = "Document title") },
            enabled = !state.isLoading && !state.isSavingMetadata,
            singleLine = true
        )

        Text(text = "Folder")
        Button(
            onClick = { onFolderSelected(null) },
            enabled = !state.isLoading && !state.isSavingMetadata
        ) {
            val rootLabel = if (state.selectedFolderId == null) {
                "No Folder (Selected)"
            } else {
                "No Folder"
            }
            Text(text = rootLabel)
        }
        state.availableFolders.forEach { folder ->
            Button(
                onClick = { onFolderSelected(folder.id) },
                enabled = !state.isLoading && !state.isSavingMetadata
            ) {
                val folderLabel = if (folder.id == state.selectedFolderId) {
                    "${folder.name} (Selected)"
                } else {
                    folder.name
                }
                Text(text = folderLabel)
            }
        }

        Button(
            onClick = onSaveDetailsClick,
            enabled = state.canSaveMetadata && !state.isLoading && !state.isSavingMetadata
        ) {
            Text(text = if (state.isSavingMetadata) "Saving..." else "Save Details")
        }

        state.errorMessage?.let { errorMessage ->
            Text(text = errorMessage)
        }

        state.generatedPdf?.let { generatedPdf ->
            Text(text = "Generated PDF: ${generatedPdf.name}")
        }

        Button(
            onClick = onShareClick,
            enabled = state.canShare && !state.isLoading && !state.isExporting
        ) {
            Text(text = if (state.isExporting) "Exporting..." else "Share PDF")
        }
    }
}
