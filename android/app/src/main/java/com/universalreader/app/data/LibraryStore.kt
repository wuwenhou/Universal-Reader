package com.universalreader.app.data

import android.content.Context
import com.universalreader.app.core.NativeCore

/**
 * Local library and reading progress.
 *
 * SharedPreferences + JSON is intentional for the MVP: the data set is small,
 * writes are cheap, and there is no query/migration burden yet. The codec is
 * pure Kotlin so a future Room migration can reuse the same model and tests.
 */
class LibraryStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun list(): List<BookRecord> =
        LibraryCodec.decode(prefs.getString(KEY_BOOKS, null))
            .sortedByDescending { it.lastReadAt }

    @Synchronized
    fun saveBook(
        url: String,
        title: String,
        siteName: String?,
        chapterTitle: String?,
        progress: Int = -1,
        total: Int = 0
    ) {
        val key = key(url)
        val books = LibraryCodec.decode(prefs.getString(KEY_BOOKS, null)).toMutableList()
        val index = books.indexOfFirst { it.url == key }
        val existing = books.getOrNull(index)
        val record = BookRecord(
            url = key,
            title = title.ifBlank { existing?.title ?: key },
            siteName = siteName ?: existing?.siteName,
            chapterTitle = chapterTitle ?: existing?.chapterTitle,
            lastReadAt = System.currentTimeMillis(),
            progress = if (progress >= 0) progress else existing?.progress ?: 0,
            total = if (total > 0) total else existing?.total ?: 0
        )
        if (index >= 0) books[index] = record else books.add(record)
        write(books)
    }

    @Synchronized
    fun saveProgress(url: String, progress: Int, total: Int) {
        val key = key(url)
        val books = LibraryCodec.decode(prefs.getString(KEY_BOOKS, null)).toMutableList()
        val index = books.indexOfFirst { it.url == key }
        if (index < 0) return
        books[index] = books[index].copy(
            progress = progress.coerceAtLeast(0),
            total = total.coerceAtLeast(0),
            lastReadAt = System.currentTimeMillis()
        )
        write(books)
    }

    @Synchronized
    fun progressFor(url: String): Int {
        val key = key(url)
        return LibraryCodec.decode(prefs.getString(KEY_BOOKS, null))
            .firstOrNull { it.url == key }?.progress ?: 0
    }

    @Synchronized
    fun remove(url: String) {
        val key = key(url)
        val books = LibraryCodec.decode(prefs.getString(KEY_BOOKS, null))
            .filterNot { it.url == key }
        write(books)
    }

    @Synchronized
    fun clear() {
        prefs.edit().remove(KEY_BOOKS).apply()
    }

    private fun write(books: List<BookRecord>) {
        prefs.edit().putString(KEY_BOOKS, LibraryCodec.encode(books)).apply()
    }

    private fun key(url: String): String = NativeCore.canonicalizeUrl(url) ?: url

    private companion object {
        const val PREFS_NAME = "universal_reader_library"
        const val KEY_BOOKS = "books"
    }
}
