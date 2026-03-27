package com.scanni.app.flow

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.scanni.app.MainActivity
import org.junit.Rule
import org.junit.Test

class ScanToLibraryFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun captureReviewAndSave_navigatesToLibraryWithSavedDocument() {
        composeRule.onNodeWithText("Capture Sample").performClick()
        composeRule.onNodeWithTag("review-screen").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            try {
                composeRule.onNodeWithText("Save Document").assertIsEnabled()
                true
            } catch (_: AssertionError) {
                false
            }
        }
        composeRule.onNodeWithText("Save Document").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            try {
                composeRule.onNodeWithTag("library-screen").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }
        composeRule.onNodeWithTag("library-screen").assertIsDisplayed()
        composeRule.onNodeWithText("Quick Scan").assertIsDisplayed()
    }

    @Test
    fun captureAnotherPage_thenSave_createsTwoPageDocument() {
        composeRule.onNodeWithText("Capture Sample").performClick()
        composeRule.onNodeWithTag("review-screen").assertIsDisplayed()
        composeRule.onNodeWithText("Add Another Page").performClick()
        composeRule.onNodeWithTag("scanner-screen").assertIsDisplayed()

        composeRule.onNodeWithText("Capture Sample").performClick()
        composeRule.onNodeWithTag("review-screen").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            try {
                composeRule.onNodeWithText("Save Document").assertIsEnabled()
                true
            } catch (_: AssertionError) {
                false
            }
        }

        composeRule.onNodeWithText("Save Document").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            try {
                composeRule.onNodeWithTag("library-screen").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }

        composeRule.onAllNodesWithText("Quick Scan")[0].performClick()
        composeRule.onNodeWithTag("document-detail-screen").assertIsDisplayed()
        composeRule.onNodeWithText("Pages: 2").assertIsDisplayed()
    }
}
