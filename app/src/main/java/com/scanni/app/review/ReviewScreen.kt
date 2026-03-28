package com.scanni.app.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.scanni.app.processing.EnhancementMode

@Composable
fun ReviewScreen(
    state: ReviewUiState,
    onModeChange: (EnhancementMode) -> Unit,
    onAddAnotherPageClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    val activePage = state.activePage
    Column(modifier = Modifier.testTag("review-screen")) {
        Text(text = "Pages: ${state.pages.size}")
        Text(text = "Active Page: ${state.activePageIndex + 1}")
        Text(text = "Mode: ${activePage?.mode?.name ?: "NONE"}")
        Text(text = "Original: ${activePage?.originalPath ?: ""}")
        Text(text = "Processed: ${activePage?.processedPath ?: ""}")
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
            enabled = activePage != null && activePage.processedPath.isNotBlank() && !activePage.isProcessing
        ) {
            Text(text = "Save Document")
        }
        if (activePage?.isProcessing == true) {
            Text(text = "Processing...")
        }
        activePage?.errorMessage?.let { errorMessage ->
            Text(text = "Error: $errorMessage")
        }
    }
}
