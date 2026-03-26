package com.scanni.app.review

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
fun ReviewScreen(state: PageReviewState) {
    Column(modifier = Modifier.testTag("review-screen")) {
        Text(text = "Mode: ${state.mode.name}")
        Text(text = "Original: ${state.originalPath}")
        Text(text = "Processed: ${state.processedPath}")
        if (state.isProcessing) {
            Text(text = "Processing...")
        }
        state.errorMessage?.let { errorMessage ->
            Text(text = "Error: $errorMessage")
        }
    }
}
