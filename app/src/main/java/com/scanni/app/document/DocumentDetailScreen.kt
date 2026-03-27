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

data class DocumentDetailUiState(
    val title: String,
    val pageCount: Int,
    val ocrStatus: String
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
        Text(
            text = state.title,
            style = MaterialTheme.typography.headlineSmall
        )
        Text(text = "Pages: ${state.pageCount}")
        Text(text = "OCR: ${state.ocrStatus}")
        Button(onClick = onShareClick) {
            Text(text = "Share PDF")
        }
    }
}
