package com.scanni.app.library

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class LibraryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun libraryScreen_showsSearchResults() {
        composeRule.setContent {
            LibraryScreen(
                state = LibraryUiState(
                    documents = listOf(
                        LibraryDocumentItem(
                            id = 1L,
                            title = "Physics Notes",
                            pageCount = 12
                        )
                    )
                ),
                onQueryChange = {},
                onFolderClick = {},
                onDocumentClick = {}
            )
        }

        composeRule.onNodeWithText("Physics Notes").assertIsDisplayed()
    }

    @Test
    fun libraryScreen_showsFolders() {
        composeRule.setContent {
            LibraryScreen(
                state = LibraryUiState(
                    folders = listOf(
                        LibraryFolderItem(
                            id = 7L,
                            name = "Semester 2"
                        )
                    )
                ),
                onQueryChange = {},
                onFolderClick = {},
                onDocumentClick = {}
            )
        }

        composeRule.onNodeWithText("Semester 2").assertIsDisplayed()
    }
}
