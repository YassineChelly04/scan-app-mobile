package com.scanni.app.camera

data class CapturedPageDraft(
    val originalPath: String,
    val previewPath: String,
    val detectedCorners: List<Float>
)
