package com.universalreader.app.reader

import com.universalreader.app.core.NativeCore

/**
 * Bounded visited-URL set used by automatic next-chapter navigation.
 *
 * URLs are canonicalized by the Rust core, so tracking parameters and
 * fragments cannot trick the guard into visiting the same chapter twice.
 */
class ChapterGuard(private val maxEntries: Int = 200) {
    private val visited = LinkedHashSet<String>()

    fun markVisited(url: String): Boolean {
        val key = NativeCore.canonicalizeUrl(url) ?: return false
        if (visited.size >= maxEntries) {
            val iterator = visited.iterator()
            if (iterator.hasNext()) {
                iterator.next()
                iterator.remove()
            }
        }
        return visited.add(key)
    }

    fun canVisit(url: String): Boolean {
        val key = NativeCore.canonicalizeUrl(url) ?: return false
        return key !in visited
    }

    fun isVisited(url: String): Boolean {
        val key = NativeCore.canonicalizeUrl(url) ?: return false
        return key in visited
    }

    fun clear() = visited.clear()

    fun size(): Int = visited.size
}
