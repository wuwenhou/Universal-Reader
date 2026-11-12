package com.universalreader.app.tts

/** Pure playback cursor used by the TTS service and unit tests. */
class PlaybackQueue(items: List<String>, startIndex: Int = 0) {
    val items: List<String> = items.toList()

    var index: Int = if (this.items.isEmpty()) {
        -1
    } else {
        startIndex.coerceIn(0, this.items.lastIndex)
    }
        private set

    val size: Int get() = items.size
    val current: String? get() = items.getOrNull(index)
    val hasNext: Boolean get() = index + 1 < items.size
    val hasPrevious: Boolean get() = index > 0
    val isFinished: Boolean get() = items.isEmpty() || index >= items.size

    fun advance(): Boolean {
        if (!hasNext) return false
        index += 1
        return true
    }

    fun previous(): Boolean {
        if (!hasPrevious) return false
        index -= 1
        return true
    }

    fun jumpTo(position: Int): Boolean {
        if (position !in items.indices) return false
        index = position
        return true
    }
}
