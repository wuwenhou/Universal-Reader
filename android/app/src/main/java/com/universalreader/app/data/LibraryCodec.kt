package com.universalreader.app.data

import org.json.JSONArray
import org.json.JSONObject

/** JSON codec for [BookRecord]. Kept pure so it can be unit-tested on the JVM. */
object LibraryCodec {
    fun encode(books: List<BookRecord>): String {
        val array = JSONArray()
        books.forEach { book ->
            array.put(
                JSONObject().apply {
                    put("url", book.url)
                    put("title", book.title)
                    put("siteName", book.siteName ?: JSONObject.NULL)
                    put("chapterTitle", book.chapterTitle ?: JSONObject.NULL)
                    put("lastReadAt", book.lastReadAt)
                    put("progress", book.progress)
                    put("total", book.total)
                }
            )
        }
        return array.toString()
    }

    fun decode(raw: String?): List<BookRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val url = item.optString("url")
                if (url.isBlank()) continue
                add(
                    BookRecord(
                        url = url,
                        title = item.optString("title").ifBlank { url },
                        siteName = item.optionalString("siteName"),
                        chapterTitle = item.optionalString("chapterTitle"),
                        lastReadAt = item.optLong("lastReadAt"),
                        progress = item.optInt("progress").coerceAtLeast(0),
                        total = item.optInt("total").coerceAtLeast(0)
                    )
                )
            }
        }
    }

    private fun JSONObject.optionalString(key: String): String? {
        if (isNull(key)) return null
        return optString(key).takeIf { it.isNotBlank() && it != "null" }
    }
}
