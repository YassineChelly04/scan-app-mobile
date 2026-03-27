package com.scanni.app.camera

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class ScannerScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun missingCameraPermission_showsGrantActionInsteadOfPreview() {
        composeRule.setContent {
            ScannerScreen(
                state = ScannerUiState(),
                hasCameraPermission = false,
                cameraPreview = {
                    Box(
                        modifier = Modifier
                            .size(1.dp)
                            .testTag("camera-preview")
                    )
                },
                onGrantCameraAccessClick = {},
                onCameraCaptureClick = {},
                onSampleCaptureClick = {},
                onLibraryClick = {}
            )
        }

        composeRule.onNodeWithText("Grant Camera Access").assertIsDisplayed()
        composeRule.onAllNodesWithTag("camera-preview").assertCountEquals(0)
    }

    @Test
    fun grantedCameraPermission_showsPreviewAndCameraCaptureAction() {
        composeRule.setContent {
            ScannerScreen(
                state = ScannerUiState(),
                hasCameraPermission = true,
                cameraPreview = {
                    Box(
                        modifier = Modifier
                            .size(1.dp)
                            .testTag("camera-preview")
                    )
                },
                onGrantCameraAccessClick = {},
                onCameraCaptureClick = {},
                onSampleCaptureClick = {},
                onLibraryClick = {}
            )
        }

        composeRule.onNodeWithTag("camera-preview").assertIsDisplayed()
        composeRule.onNodeWithText("Capture Camera").assertIsDisplayed()
        composeRule.onAllNodesWithTag("grant-camera-access").assertCountEquals(0)
    }
}
