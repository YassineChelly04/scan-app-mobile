package com.scanni.app.camera

data class ScannerUiState(
    val pages: List<CapturedPageDraft> = emptyList(),
    val captureCount: Int = 0
)
