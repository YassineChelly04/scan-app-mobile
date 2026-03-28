package com.scanni.app.review

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.scanni.app.processing.EnhancementMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReviewScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pageSwitcher_updatesTheActivePageMode() {
        var state by mutableStateOf(
            ReviewUiState(
                pages = listOf(
                    ReviewPageState(
                        originalPath = "files/page-1.jpg",
                        corners = listOf(0.1f, 0.1f, 0.9f, 0.1f, 0.9f, 0.9f, 0.1f, 0.9f),
                        mode = EnhancementMode.DOCUMENT,
                        processedPath = "files/page-1-document.jpg"
                    ),
                    ReviewPageState(
                        originalPath = "files/page-2.jpg",
                        corners = listOf(0.15f, 0.1f, 0.85f, 0.1f, 0.85f, 0.9f, 0.15f, 0.9f),
                        mode = EnhancementMode.BOOK,
                        processedPath = "files/page-2-book.jpg"
                    )
                )
            )
        )

        composeRule.setContent {
            ReviewScreen(
                state = state,
                onPageSelected = { state = state.copy(activePageIndex = it) },
                onModeChange = {},
                onCropChanged = {},
                onCropChangeFinished = {},
                onAddAnotherPageClick = {},
                onSaveClick = {}
            )
        }

        composeRule.onNodeWithTag("review-page-switcher").assertIsDisplayed()
        composeRule.onNodeWithText("Mode: DOCUMENT").assertIsDisplayed()
        composeRule.onNodeWithTag("review-page-button-1").performClick()
        composeRule.onNodeWithText("Mode: BOOK").assertIsDisplayed()
    }

    @Test
    fun cropHandleDrag_updatesCornersAndSignalsCompletion() {
        var state by mutableStateOf(
            ReviewUiState(
                pages = listOf(
                    ReviewPageState(
                        originalPath = "files/page-1.jpg",
                        corners = listOf(0.2f, 0.2f, 0.8f, 0.2f, 0.8f, 0.8f, 0.2f, 0.8f),
                        mode = EnhancementMode.DOCUMENT,
                        processedPath = "files/page-1-document.jpg"
                    )
                )
            )
        )
        var completedCount by mutableIntStateOf(0)

        composeRule.setContent {
            ReviewScreen(
                state = state,
                onPageSelected = {},
                onModeChange = {},
                onCropChanged = { corners ->
                    state = state.copy(
                        pages = state.pages.toMutableList().also { pages ->
                            pages[state.activePageIndex] = pages[state.activePageIndex].copy(corners = corners)
                        }
                    )
                },
                onCropChangeFinished = { completedCount += 1 },
                onAddAnotherPageClick = {},
                onSaveClick = {}
            )
        }

        composeRule.onNodeWithTag("crop-handle-0").assertIsDisplayed()
        composeRule.onNodeWithTag("crop-handle-0").performTouchInput {
            down(center)
            moveBy(Offset(30f, 20f))
            up()
        }

        composeRule.runOnIdle {
            assertTrue(state.pages[0].corners[0] > 0.2f)
            assertTrue(state.pages[0].corners[1] > 0.2f)
            assertEquals(1, completedCount)
        }
    }
}
