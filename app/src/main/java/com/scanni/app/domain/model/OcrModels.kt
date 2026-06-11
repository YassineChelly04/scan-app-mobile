package com.scanni.app.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** One recognized word with its bounding box, normalized to the processed page image. */
@Serializable
data class OcrWord(
    val text: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

data class OcrResult(
    val text: String,
    val words: List<OcrWord>,
) {
    companion object {
        val EMPTY = OcrResult("", emptyList())
    }
}

object OcrWords {
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(OcrWord.serializer())

    fun encode(words: List<OcrWord>): String = json.encodeToString(serializer, words)

    fun decode(value: String?): List<OcrWord> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString(serializer, value) }.getOrDefault(emptyList())
    }
}
