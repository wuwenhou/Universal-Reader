package com.universalreader.app.data

/** A book/chapter saved in the local library. */
data class BookRecord(
    val url: String,
    val title: String,
    val siteName: String? = null,
    val chapterTitle: String? = null,
    val lastReadAt: Long = 0L,
    val progress: Int = 0,
    val total: Int = 0
) {
    val percent: Int
        get() = if (total <= 0) 0 else (progress * 100 / total).coerceIn(0, 100)
}
