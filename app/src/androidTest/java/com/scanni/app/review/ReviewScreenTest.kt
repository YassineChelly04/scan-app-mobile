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
                        rotationQuarterTurns = 0,
                        processedPath = "files/page-1-document.jpg"
                    ),
                    ReviewPageState(
                        originalPath = "files/page-2.jpg",
                        corners = listOf(0.15f, 0.1f, 0.85f, 0.1f, 0.85f, 0.9f, 0.15f, 0.9f),
                        mode = EnhancementMode.BOOK,
                        rotationQuarterTurns = 0,
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
                onRotateLeft = {},
                onRotateRight = {},
                onDeletePage = {},
                onMovePage = { _, _ -> },
                onAddAnotherPageClick = {},
                onSaveClick = {}
            )
        }

        composeRule.onNodeWithTag("review-page-switcher").assertIsDisplayed()
        composeRule.onNodeWithText("Mode: DOCUMENT").assertIsDisplayed()
        composeRule.onNodeWithTag("review-page-card-1").performClick()
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
                        rotationQuarterTurns = 0,
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
                onRotateLeft = {},
                onRotateRight = {},
                onDeletePage = {},
                onMovePage = { _, _ -> },
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

    @Test
    fun rotateAndDeleteActionsOperateOnTheActivePage() {
        var state by mutableStateOf(
            ReviewUiState(
                pages = listOf(
                    ReviewPageState(
                        originalPath = "files/page-1.jpg",
                        corners = listOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f),
                        rotationQuarterTurns = 0,
                        processedPath = "one.jpg"
                    ),
                    ReviewPageState(
                        originalPath = "files/page-2.jpg",
                        corners = listOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f),
                        rotationQuarterTurns = 0,
                        processedPath = "two.jpg"
                    )
                ),
                activePageIndex = 1
            )
        )

        composeRule.setContent {
            ReviewScreen(
                state = state,
                onPageSelected = { state = state.copy(activePageIndex = it) },
                onModeChange = {},
                onCropChanged = {},
                onCropChangeFinished = {},
                onRotateLeft = {
                    val pages = state.pages.toMutableList()
                    val page = pages[state.activePageIndex]
                    pages[state.activePageIndex] = page.copy(rotationQuarterTurns = 3)
                    state = state.copy(pages = pages)
                },
                onRotateRight = {},
                onDeletePage = {
                    val pages = state.pages.toMutableList()
                    pages.removeAt(state.activePageIndex)
                    state = state.copy(pages = pages, activePageIndex = 0)
                },
                onMovePage = { _, _ -> },
                onAddAnotherPageClick = {},
                onSaveClick = {}
            )
        }

        composeRule.onNodeWithTag("review-rotate-left").performClick()
        composeRule.onNodeWithText("Rotation: 270°").assertIsDisplayed()
        composeRule.onNodeWithTag("review-delete-page").performClick()
        composeRule.onNodeWithText("Original: files/page-1.jpg").assertIsDisplayed()
    }

    @Test
    fun draggingPageStripItem_reordersThePages() {
        var state by mutableStateOf(
            ReviewUiState(
                pages = listOf(
                    ReviewPageState(
                        originalPath = "files/page-1.jpg",
                        corners = listOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f),
                        processedPath = "one.jpg"
                    ),
                    ReviewPageState(
                        originalPath = "files/page-2.jpg",
                        corners = listOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f),
                        processedPath = "two.jpg"
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
                onRotateLeft = {},
                onRotateRight = {},
                onDeletePage = {},
                onMovePage = { fromIndex, toIndex ->
                    val pages = state.pages.toMutableList()
                    val moved = pages.removeAt(fromIndex)
                    pages.add(toIndex, moved)
                    state = state.copy(
                        pages = pages,
                        activePageIndex = if (state.activePageIndex == fromIndex) toIndex else state.activePageIndex
                    )
                },
                onAddAnotherPageClick = {},
                onSaveClick = {}
            )
        }

        composeRule.onNodeWithTag("review-page-card-1").performTouchInput {
            down(center)
            moveBy(Offset(-250f, 0f))
            up()
        }

        composeRule.runOnIdle {
            assertEquals("files/page-2.jpg", state.pages[0].originalPath)
            assertEquals("files/page-1.jpg", state.pages[1].originalPath)
        }
    }
}
