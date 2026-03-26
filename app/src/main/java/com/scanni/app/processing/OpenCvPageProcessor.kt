package com.scanni.app.processing

class OpenCvPageProcessor : PageProcessor {
    override suspend fun process(
        originalPath: String,
        mode: EnhancementMode,
        corners: List<Float>
    ): String {
        val profile = ProcessingProfile(mode)
        return originalPath.removeSuffix(".jpg") + "-${profile.mode.name.lowercase()}.jpg"
    }
}
