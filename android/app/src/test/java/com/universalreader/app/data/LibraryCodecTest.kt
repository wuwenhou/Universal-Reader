package com.universalreader.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryCodecTest {

    @Test
    fun roundTripsAllFields() {
        val books = listOf(
            BookRecord(
                url = "https://example.org/book",
                title = "Một cuốn sách",
                siteName = "Example",
                chapterTitle = "Chương 1",
                lastReadAt = 1_700_000_000_000L,
                progress = 7,
                total = 42
            )
        )

        assertEquals(books, LibraryCodec.decode(LibraryCodec.encode(books)))
    }

    @Test
    fun decodesNullBlankAndInvalidInputAsEmpty() {
        assertTrue(LibraryCodec.decode(null).isEmpty())
        assertTrue(LibraryCodec.decode("").isEmpty())
        assertTrue(LibraryCodec.decode("not json").isEmpty())
        assertTrue(LibraryCodec.decode("{}").isEmpty())
    }

    @Test
    fun skipsEntriesWithoutUrlAndFillsDefaults() {
        val raw = """
            [
              {"title": "missing url"},
              {"url": "https://example.org/a", "title": ""},
              {"url": "https://example.org/b", "title": "B", "progress": -3, "total": -1}
            ]
        """.trimIndent()

        val books = LibraryCodec.decode(raw)
        assertEquals(2, books.size)
        assertEquals("https://example.org/a", books[0].title)
        assertEquals(0, books[1].progress)
        assertEquals(0, books[1].total)
    }

    @Test
    fun percentIsBounded() {
        assertEquals(0, BookRecord("u", "t", total = 0).percent)
        assertEquals(50, BookRecord("u", "t", progress = 5, total = 10).percent)
        assertEquals(100, BookRecord("u", "t", progress = 99, total = 10).percent)
    }
}
