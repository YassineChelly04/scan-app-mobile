package com.scanni.app.review

import com.scanni.app.processing.EnhancementMode

data class ReviewPageState(
    val originalPath: String,
    val corners: List<Float>,
    val mode: EnhancementMode = EnhancementMode.DOCUMENT,
    val processedPath: String = "",
    val isProcessing: Boolean = false,
    val errorMessage: String? = null
)
