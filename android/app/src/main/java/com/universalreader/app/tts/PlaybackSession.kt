package com.universalreader.app.tts

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** UI state for the current TTS session. */
data class TtsUiState(
    val isPlaying: Boolean = false,
    val isPreparing: Boolean = false,
    val currentIndex: Int = 0,
    val total: Int = 0,
    val rate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val voiceName: String? = null,
    val voiceOffline: Boolean? = null,
    val error: String? = null,
    val completed: Boolean = false
)

/**
 * In-process hand-off between the UI and [TtsPlaybackService].
 *
 * The queue stays in memory instead of being passed through an Intent, which
 * avoids Binder transaction limits for long chapters.
 */
object PlaybackSession {
    private val _state = MutableStateFlow(TtsUiState())
    val state: StateFlow<TtsUiState> = _state.asStateFlow()

    var queue: List<String> = emptyList()
        private set
    var bookUrl: String = ""
        private set
    var bookTitle: String = ""
        private set
    var startIndex: Int = 0
        private set
    var rate: Float = 1.0f
        private set
    var pitch: Float = 1.0f
        private set

    @Synchronized
    fun prepare(
        url: String,
        title: String,
        chunks: List<String>,
        startIndex: Int,
        rate: Float,
        pitch: Float
    ) {
        queue = chunks
        bookUrl = url
        bookTitle = title
        this.startIndex = if (chunks.isEmpty()) 0 else startIndex.coerceIn(0, chunks.lastIndex)
        this.rate = rate
        this.pitch = pitch
        _state.value = TtsUiState(
            isPreparing = true,
            currentIndex = this.startIndex,
            total = chunks.size,
            rate = rate,
            pitch = pitch
        )
    }

    fun update(transform: (TtsUiState) -> TtsUiState) {
        _state.update(transform)
    }

    @Synchronized
    fun setRate(value: Float) {
        rate = value.coerceIn(0.5f, 2.0f)
        _state.update { it.copy(rate = rate) }
    }

    @Synchronized
    fun setPitch(value: Float) {
        pitch = value.coerceIn(0.5f, 2.0f)
        _state.update { it.copy(pitch = pitch) }
    }

    /** Returns true once for each completed queue, then clears the flag. */
    fun consumeCompletion(): Boolean {
        if (!_state.value.completed) return false
        _state.update { it.copy(completed = false) }
        return true
    }

    @Synchronized
    fun reset() {
        queue = emptyList()
        bookUrl = ""
        bookTitle = ""
        startIndex = 0
        _state.value = TtsUiState()
    }
}
