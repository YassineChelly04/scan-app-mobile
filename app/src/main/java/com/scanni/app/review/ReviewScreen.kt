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
    state: PageReviewState,
    onModeChange: (EnhancementMode) -> Unit,
    onSaveClick: () -> Unit
) {
    Column(modifier = Modifier.testTag("review-screen")) {
        Text(text = "Mode: ${state.mode.name}")
        Text(text = "Original: ${state.originalPath}")
        Text(text = "Processed: ${state.processedPath}")
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
        Button(
            onClick = onSaveClick,
            enabled = state.processedPath.isNotBlank() && !state.isProcessing
        ) {
            Text(text = "Save Document")
        }
        if (state.isProcessing) {
            Text(text = "Processing...")
        }
        state.errorMessage?.let { errorMessage ->
            Text(text = "Error: $errorMessage")
        }
    }
}
