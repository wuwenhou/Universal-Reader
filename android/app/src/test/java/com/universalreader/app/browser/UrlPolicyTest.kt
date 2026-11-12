package com.universalreader.app.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlPolicyTest {

    @Test
    fun acceptsHttpsAndAddsSchemeWhenMissing() {
        assertEquals("https://example.org", UrlPolicy.normalizeInput("example.org"))
        assertEquals(
            "https://example.org/article?q=1#part",
            UrlPolicy.normalizeInput("https://example.org/article?q=1#part")
        )
    }

    @Test
    fun rejectsEveryNonHttpsScheme() {
        assertNull(UrlPolicy.normalizeInput("http://example.org"))
        assertNull(UrlPolicy.normalizeInput("javascript:alert(1)"))
        assertNull(UrlPolicy.normalizeInput("file:///etc/passwd"))
        assertNull(UrlPolicy.normalizeInput("content://com.example/file"))
        assertNull(UrlPolicy.normalizeInput("data:text/html,hello"))
        assertNull(UrlPolicy.normalizeInput("intent://scan/#Intent;scheme=zxing;end"))
        assertNull(UrlPolicy.normalizeInput(""))
    }

    @Test
    fun rejectsHttpsWithoutHost() {
        assertNull(UrlPolicy.normalizeInput("https://"))
        assertNull(UrlPolicy.normalizeInput("https:///path"))
    }

    @Test
    fun plainWordsBecomeASearch() {
        assertEquals(
            "https://duckduckgo.com/?q=hello+world",
            UrlPolicy.resolveInput("hello world")
        )
        assertEquals(
            "https://duckduckgo.com/?q=kotlin",
            UrlPolicy.resolveInput("kotlin")
        )
    }

    @Test
    fun urlLikeInputIsNotSearched() {
        assertEquals(
            "https://example.org/novel/chapter-1",
            UrlPolicy.resolveInput("example.org/novel/chapter-1")
        )
    }

    @Test
    fun allowedCheckMatchesNormalization() {
        assertTrue(UrlPolicy.isAllowed("https://example.org"))
        assertFalse(UrlPolicy.isAllowed("http://example.org"))
        assertFalse(UrlPolicy.isAllowed("javascript:alert(1)"))
        assertFalse(UrlPolicy.isAllowed("https://"))
    }

    @Test
    fun hostIsLowercased() {
        assertEquals("www.example.org", UrlPolicy.hostOf("https://WWW.Example.ORG/x"))
        assertNull(UrlPolicy.hostOf("not a url"))
    }
}
