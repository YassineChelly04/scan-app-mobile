package com.scanni.app.document

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class DocumentDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun documentDetailScreen_showsShareAction() {
        composeRule.setContent {
            DocumentDetailScreen(
                state = DocumentDetailUiState(
                    title = "Lecture Notes",
                    pageCount = 3,
                    ocrStatus = "complete",
                    isLoading = false,
                    canShare = true
                ),
                onShareClick = {}
            )
        }

        composeRule.onNodeWithText("Share PDF").assertIsDisplayed()
    }

    @Test
    fun documentDetailScreen_showsErrorMessage() {
        composeRule.setContent {
            DocumentDetailScreen(
                state = DocumentDetailUiState(
                    title = "Lecture Notes",
                    pageCount = 3,
                    ocrStatus = "complete",
                    isLoading = false,
                    canShare = false,
                    errorMessage = "Processed page file missing."
                ),
                onShareClick = {}
            )
        }

        composeRule.onNodeWithText("Processed page file missing.").assertIsDisplayed()
    }
}
