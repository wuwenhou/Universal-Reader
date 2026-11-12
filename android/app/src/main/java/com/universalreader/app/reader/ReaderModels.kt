package com.universalreader.app.reader

/** Where the reader text came from. */
enum class ExtractionSource {
    SITE_ADAPTER,
    READABILITY,
    SELECTION
}

/** A single extracted chapter/article, ready for the reader and TTS. */
data class ReaderContent(
    val url: String,
    val title: String,
    val byline: String? = null,
    val siteName: String? = null,
    val paragraphs: List<String> = emptyList(),
    val nextUrl: String? = null,
    val source: ExtractionSource = ExtractionSource.READABILITY
) {
    val wordCount: Int
        get() = paragraphs.sumOf { paragraph ->
            paragraph.split(Regex("\\s+")).count { it.isNotBlank() }
        }
}

/** Result of an extraction attempt. Exactly one field is non-null. */
data class ExtractionResult(
    val content: ReaderContent? = null,
    val error: String? = null
)
