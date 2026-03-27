package com.scanni.app.flow

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
        composeRule.onNodeWithText("Save Document").performClick()
        composeRule.onNodeWithTag("library-screen").assertIsDisplayed()
        composeRule.onNodeWithText("Quick Scan").assertIsDisplayed()
    }
}
