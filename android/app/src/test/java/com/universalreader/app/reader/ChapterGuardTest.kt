package com.universalreader.app.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChapterGuardTest {

    @Test
    fun trackingParametersAndFragmentsDoNotCreateDuplicates() {
        val guard = ChapterGuard()
        assertTrue(guard.markVisited("https://example.org/ch1?utm_source=feed"))
        assertFalse(guard.canVisit("https://example.org/ch1#top"))
        assertFalse(guard.markVisited("https://example.org/ch1"))
        assertTrue(guard.canVisit("https://example.org/ch2"))
    }

    @Test
    fun unsafeUrlsAreNeverVisited() {
        val guard = ChapterGuard()
        assertFalse(guard.markVisited("javascript:alert(1)"))
        assertFalse(guard.canVisit("file:///etc/passwd"))
        assertFalse(guard.isVisited("data:text/html,hello"))
        assertEquals(0, guard.size())
    }

    @Test
    fun oldestEntriesAreEvictedAtTheBound() {
        val guard = ChapterGuard(maxEntries = 2)
        guard.markVisited("https://example.org/1")
        guard.markVisited("https://example.org/2")
        guard.markVisited("https://example.org/3")

        assertEquals(2, guard.size())
        assertTrue(guard.canVisit("https://example.org/1"))
        assertFalse(guard.canVisit("https://example.org/2"))
        assertFalse(guard.canVisit("https://example.org/3"))
    }

    @Test
    fun clearForgetsEverything() {
        val guard = ChapterGuard()
        guard.markVisited("https://example.org/ch1")
        guard.clear()
        assertTrue(guard.canVisit("https://example.org/ch1"))
    }
}
