package com.scanni.app.processing

interface PageProcessor {
    suspend fun process(
        originalPath: String,
        mode: EnhancementMode,
        corners: List<Float>
    ): String
}
