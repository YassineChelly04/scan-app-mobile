package com.scanni.app.review

import com.scanni.app.processing.EnhancementMode

data class PageReviewState(
    val originalPath: String = "",
    val processedPath: String = "",
    val mode: EnhancementMode = EnhancementMode.DOCUMENT,
    val corners: List<Float> = emptyList(),
    val isProcessing: Boolean = false,
    val errorMessage: String? = null
)
