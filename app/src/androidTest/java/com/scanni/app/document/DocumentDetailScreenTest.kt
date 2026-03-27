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

    @Test
    fun documentDetailScreen_showsMetadataEditingControls() {
        composeRule.setContent {
            DocumentDetailScreen(
                state = DocumentDetailUiState(
                    title = "Lecture Notes",
                    editableTitle = "Lecture Notes",
                    pageCount = 3,
                    ocrStatus = "pending",
                    isLoading = false,
                    canShare = true,
                    canSaveMetadata = true,
                    selectedFolderId = 7L,
                    availableFolders = listOf(
                        DocumentFolderOption(id = 7L, name = "Semester 2")
                    )
                ),
                onTitleChange = {},
                onFolderSelected = {},
                onSaveDetailsClick = {},
                onShareClick = {}
            )
        }

        composeRule.onNodeWithText("Save Details").assertIsDisplayed()
        composeRule.onNodeWithText("Semester 2 (Selected)").assertIsDisplayed()
    }
}
