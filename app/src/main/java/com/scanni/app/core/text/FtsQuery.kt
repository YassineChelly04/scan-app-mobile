package com.scanni.app.core.text

/** Builds safe SQLite FTS MATCH expressions from raw user input. */
object FtsQuery {

    private val tokenSplitter = Regex("[^\\p{L}\\p{N}]+")
    private const val MAX_TOKENS = 8

    /**
     * Returns a MATCH expression where every token is required (implicit AND) and
     * the last token matches by prefix, or null when the input holds no tokens.
     * All FTS operator characters are stripped, so user input cannot inject syntax.
     */
    fun sanitize(raw: String): String? {
        val tokens = raw.split(tokenSplitter)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(MAX_TOKENS)
        if (tokens.isEmpty()) return null
        return tokens.mapIndexed { index, token ->
            if (index == tokens.lastIndex) "$token*" else "\"$token\""
        }.joinToString(" ")
    }

    /** Argument for `LIKE ? ESCAPE '\'` clauses matching anywhere in the column. */
    fun likePattern(raw: String): String {
        val escaped = raw
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
        return "%$escaped%"
    }
}
