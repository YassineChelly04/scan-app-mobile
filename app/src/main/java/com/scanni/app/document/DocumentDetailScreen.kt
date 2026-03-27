package com.scanni.app.document

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.io.File

data class DocumentDetailUiState(
    val title: String = "",
    val pageCount: Int = 0,
    val ocrStatus: String = "",
    val isLoading: Boolean = true,
    val isExporting: Boolean = false,
    val canShare: Boolean = false,
    val errorMessage: String? = null,
    val generatedPdf: File? = null
)

@Composable
fun DocumentDetailScreen(
    state: DocumentDetailUiState,
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
