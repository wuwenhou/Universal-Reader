package com.universalreader.app.core

import java.net.URI

/**
 * Safe wrapper around [NativeBridge].
 *
 * The app must still be usable when the native library is missing or built for
 * a different ABI, so every call has a small Kotlin fallback. The fallback is
 * intentionally conservative: it is only a degradation path, not the primary
 * implementation.
 */
object NativeCore {
    val available: Boolean by lazy {
        runCatching { NativeBridge.coreVersion() }.isSuccess
    }

    fun version(): Int =
        runCatching { NativeBridge.coreVersion() }.getOrDefault(-1)

    fun normalizeParagraphs(text: String): List<String> =
        runCatching { NativeBridge.normalizeParagraphs(text).toList() }
            .getOrElse { fallbackNormalize(text) }

    fun ttsChunks(text: String, maxChars: Int): List<String> =
        runCatching { NativeBridge.ttsChunks(text, maxChars).toList() }
            .getOrElse { fallbackChunks(text, maxChars) }

    fun canonicalizeUrl(url: String): String? =
        runCatching { NativeBridge.canonicalizeUrl(url) }
            .getOrNull()
            ?: fallbackCanonicalize(url)

    private fun fallbackNormalize(text: String): List<String> =
        text.split(Regex("\\n\\s*\\n"))
            .map { it.replace(Regex("\\s+"), " ").trim() }
            .filter { it.isNotEmpty() }

    private fun fallbackChunks(text: String, maxChars: Int): List<String> {
        if (maxChars <= 0) return emptyList()
        val chunks = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            var end = minOf(start + maxChars, text.length)
            if (end < text.length) {
                val window = text.substring(start, end)
                val sentence = window.indexOfLast { it in ".!?…。！？;:" }
                end = when {
                    sentence > 0 -> start + sentence + 1
                    else -> {
                        val space = window.indexOfLast { it.isWhitespace() }
                        if (space > 0) start + space + 1 else end
                    }
                }
            }
            val chunk = text.substring(start, end).trim()
            if (chunk.isNotEmpty()) chunks.add(chunk)
            start = maxOf(end, start + 1)
        }
        return chunks
    }

    private val trackingParams = setOf(
        "fbclid", "gclid", "dclid", "yclid", "msclkid",
        "mc_cid", "mc_eid", "igshid", "mkt_tok"
    )

    private fun fallbackCanonicalize(input: String): String? {
        val uri = runCatching { URI(input.trim()) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        val host = uri.host?.lowercase() ?: return null

        val port = when {
            uri.port == -1 -> ""
            scheme == "https" && uri.port == 443 -> ""
            scheme == "http" && uri.port == 80 -> ""
            else -> ":${uri.port}"
        }
        val path = uri.rawPath?.ifBlank { "/" } ?: "/"
        val query = (uri.rawQuery ?: "")
            .split('&')
            .filter { it.isNotBlank() }
            .filterNot { pair ->
                val key = pair.substringBefore('=').lowercase()
                key.startsWith("utm_") || key in trackingParams
            }
            .sorted()
            .joinToString("&")

        return buildString {
            append(scheme).append("://").append(host).append(port).append(path)
            if (query.isNotEmpty()) append('?').append(query)
        }
    }
}
