package com.scanni.app.camera

interface ScannerCameraController {
    suspend fun capturePage(): CapturedPageDraft
}
