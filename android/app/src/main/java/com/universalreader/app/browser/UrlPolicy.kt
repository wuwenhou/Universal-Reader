package com.universalreader.app.browser

import java.net.URI
import java.net.URLEncoder

/**
 * URL policy for the embedded browser.
 *
 * Only HTTPS is allowed. Everything that can escape the WebView sandbox or
 * execute local code (`javascript:`, `file:`, `content:`, `data:`, `intent:`,
 * `blob:`, `about:`) is rejected before it reaches `loadUrl`.
 */
object UrlPolicy {
    private const val SEARCH_PREFIX = "https://duckduckgo.com/?q="
    private val schemePattern = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")

    /** Normalizes a user-entered URL, or returns null when it is not allowed. */
    fun normalizeInput(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        val hasScheme = schemePattern.containsMatchIn(trimmed)
        if (hasScheme && !trimmed.startsWith("https://", ignoreCase = true)) {
            return null
        }

        val withScheme = if (hasScheme) trimmed else "https://$trimmed"
        val uri = runCatching { URI(withScheme) }.getOrNull() ?: return null
        if (uri.scheme?.lowercase() != "https") return null
        if (uri.host.isNullOrBlank()) return null
        return uri.toString()
    }

    /**
     * Resolves address-bar input: URLs are normalized, plain words become a
     * DuckDuckGo search. Returns null for blocked schemes or malformed input.
     */
    fun resolveInput(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        val looksLikeUrl = trimmed.contains("://") ||
            (trimmed.contains('.') && !trimmed.contains(' ')) ||
            schemePattern.containsMatchIn(trimmed)

        if (looksLikeUrl) return normalizeInput(trimmed)

        val query = runCatching { URLEncoder.encode(trimmed, "UTF-8") }.getOrNull() ?: return null
        return SEARCH_PREFIX + query
    }

    fun isAllowed(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        return uri.scheme?.lowercase() == "https" && !uri.host.isNullOrBlank()
    }

    fun hostOf(url: String): String? =
        runCatching { URI(url).host?.lowercase() }.getOrNull()
}
