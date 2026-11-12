package com.universalreader.app.browser

import android.annotation.SuppressLint
import android.app.Application
import android.os.Bundle
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.universalreader.app.core.NativeCore
import com.universalreader.app.data.BookRecord
import com.universalreader.app.data.LibraryStore
import com.universalreader.app.data.SettingsStore
import com.universalreader.app.net.AdBlocker
import com.universalreader.app.reader.ChapterGuard
import com.universalreader.app.reader.ExtractionResult
import com.universalreader.app.reader.ReaderContent
import com.universalreader.app.reader.ReadabilityExtractor
import com.universalreader.app.reader.SiteAdapterRegistry
import com.universalreader.app.tts.PlaybackSession
import com.universalreader.app.tts.TtsPlaybackService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class Screen {
    BROWSER,
    READER,
    LIBRARY,
    SETTINGS
}

data class BrowserUiState(
    val address: String = DEFAULT_URL,
    val currentUrl: String = "",
    val title: String = "",
    val progress: Int = 0,
    val loading: Boolean = false,
    val error: String? = null,
    val screen: Screen = Screen.BROWSER,
    val reader: ReaderContent? = null,
    val readerLoading: Boolean = false,
    val adBlockEnabled: Boolean = true,
    val adBlockException: Boolean = false,
    val library: List<BookRecord> = emptyList(),
    val autoNextChapter: Boolean = true,
    val fontScale: Float = 1.0f,
    val coreVersion: Int = NativeCore.version(),
    val nativeAvailable: Boolean = NativeCore.available
) {
    companion object {
        const val DEFAULT_URL = "https://duckduckgo.com/"
    }
}

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = SettingsStore(application)
    private val library = LibraryStore(application)
    private val extractor = ReadabilityExtractor(application)
    private val chapterGuard = ChapterGuard()

    private val _state = MutableStateFlow(
        BrowserUiState(
            adBlockEnabled = settings.adBlockEnabled,
            autoNextChapter = settings.autoNextChapter,
            fontScale = settings.readerFontScale
        )
    )
    val state: StateFlow<BrowserUiState> = _state.asStateFlow()

    // The reference is cleared in detach(), which Compose calls from
    // AndroidView.onRelease. It is a lifecycle-bound view holder, not a
    // long-lived cache.
    @SuppressLint("StaticFieldLeak")
    private var webView: WebView? = null
    private var pendingAutoReader = false
    private var pendingAutoPlay = false
    private var lastHost: String? = null

    var savedWebState: Bundle? = null
        private set
    var savedScrollY: Int = 0
        private set

    init {
        viewModelScope.launch {
            PlaybackSession.state.collect { tts ->
                if (tts.completed && settings.autoNextChapter &&
                    PlaybackSession.consumeCompletion()
                ) {
                    pendingAutoPlay = true
                    nextChapter(auto = true)
                }
            }
        }
    }

    // ---------------------------------------------------------------- WebView

    fun attach(webView: WebView) {
        this.webView = webView
    }

    fun detach(webView: WebView) {
        if (this.webView === webView) this.webView = null
        savedScrollY = webView.scrollY
        savedWebState = Bundle().also { bundle ->
            runCatching { webView.saveState(bundle) }
        }
    }

    /** Restores WebView history after rotation, or loads the current URL. */
    fun restoreOrLoad(webView: WebView) {
        val restored = savedWebState?.let { bundle ->
            runCatching { webView.restoreState(bundle) }.getOrNull()
        }
        savedWebState = null

        if (restored == null) {
            val url = _state.value.currentUrl.ifBlank { _state.value.address }
            if (UrlPolicy.isAllowed(url)) webView.loadUrl(url)
        }

        val scroll = savedScrollY
        savedScrollY = 0
        if (scroll > 0) {
            webView.post { webView.scrollTo(0, scroll) }
        }
    }

    fun onAddressChange(value: String) {
        _state.update { it.copy(address = value) }
    }

    fun navigate(raw: String, autoReader: Boolean = false) {
        val target = UrlPolicy.resolveInput(raw)
        if (target == null || !UrlPolicy.isAllowed(target)) {
            pendingAutoPlay = false
            _state.update {
                it.copy(error = "URL không hợp lệ hoặc không được phép. Chỉ hỗ trợ HTTPS.")
            }
            return
        }

        val host = UrlPolicy.hostOf(target)
        if (host != null && lastHost != null && host != lastHost) {
            chapterGuard.clear()
        }
        if (host != null) lastHost = host

        pendingAutoReader = autoReader
        pendingAutoPlay = false
        _state.update {
            it.copy(
                address = target,
                currentUrl = target,
                error = null,
                screen = Screen.BROWSER
            )
        }
        val view = webView
        if (view == null) {
            _state.update { it.copy(error = "WebView chưa sẵn sàng. Hãy thử lại.") }
        } else {
            view.loadUrl(target)
        }
    }

    fun reload() {
        webView?.reload()
    }

    fun stopLoading() {
        webView?.stopLoading()
    }

    fun goBack() {
        webView?.goBack()
    }

    fun goForward() {
        webView?.goForward()
    }

    fun onPageStarted(url: String) {
        if (!UrlPolicy.isAllowed(url)) return
        val host = UrlPolicy.hostOf(url)
        _state.update {
            it.copy(
                currentUrl = url,
                address = url,
                loading = true,
                progress = 0,
                error = null,
                adBlockException = settings.isAdBlockException(host),
                readerLoading = if (pendingAutoReader) it.readerLoading else false
            )
        }
    }

    fun onUrlChanged(url: String) {
        if (!UrlPolicy.isAllowed(url)) return
        _state.update { it.copy(currentUrl = url, address = url) }
    }

    fun onPageFinished(url: String) {
        _state.update {
            it.copy(currentUrl = url, address = url, loading = false, progress = 100)
        }
        val view = webView ?: return
        if (isAdBlockActive()) injectCosmeticFilters(view)
        if (pendingAutoReader) {
            pendingAutoReader = false
            openReader(force = true)
        }
    }

    fun onProgress(progress: Int) {
        val value = progress.coerceIn(0, 100)
        _state.update { it.copy(progress = value, loading = value < 100) }
    }

    fun onPageTitle(title: String) {
        if (title.isNotBlank()) _state.update { it.copy(title = title) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun onPageError(message: String) {
        pendingAutoReader = false
        pendingAutoPlay = false
        _state.update { it.copy(loading = false, readerLoading = false, error = message) }
    }

    fun onBlockedUrl(url: String) {
        _state.update {
            it.copy(error = "Đã chặn liên kết không an toàn: $url")
        }
    }

    fun shouldBlockRequest(url: String): Boolean {
        val current = _state.value
        if (!current.adBlockEnabled || current.adBlockException) return false
        return AdBlocker.shouldBlock(url)
    }

    // ----------------------------------------------------------------- Reader

    fun openReader(force: Boolean = false) {
        val view = webView ?: run {
            pendingAutoPlay = false
            return
        }
        val url = view.url ?: _state.value.currentUrl
        if (!UrlPolicy.isAllowed(url)) {
            pendingAutoPlay = false
            _state.update { it.copy(error = "Trang hiện tại không hợp lệ để trích xuất.") }
            return
        }
        if (!force && _state.value.readerLoading) return

        _state.update { it.copy(readerLoading = true, error = null) }
        val adapter = SiteAdapterRegistry.forUrl(url)
        extractor.extract(view, url, adapter) { result ->
            val content = result.content
            if (content != null) {
                finishReader(content)
            } else {
                extractor.extractSelection(view, url, _state.value.title) { selection ->
                    val selected = selection.content
                    if (selected != null) {
                        finishReader(selected)
                    } else {
                        pendingAutoPlay = false
                        _state.update {
                            it.copy(
                                readerLoading = false,
                                error = selection.error ?: result.error ?: "Không trích xuất được nội dung."
                            )
                        }
                    }
                }
            }
        }
    }

    fun readSelection() {
        val view = webView ?: run {
            pendingAutoPlay = false
            return
        }
        val url = view.url ?: _state.value.currentUrl
        if (!UrlPolicy.isAllowed(url)) {
            pendingAutoPlay = false
            return
        }
        _state.update { it.copy(readerLoading = true, error = null) }
        extractor.extractSelection(view, url, _state.value.title) { result ->
            val content = result.content
            if (content != null) {
                finishReader(content)
            } else {
                pendingAutoPlay = false
                _state.update {
                    it.copy(
                        readerLoading = false,
                        error = result.error ?: "Hãy bôi đen một đoạn văn trên trang rồi thử lại."
                    )
                }
            }
        }
    }

    private fun finishReader(content: ReaderContent) {
        chapterGuard.markVisited(content.url)
        val chunks = chunksFor(content)
        val saved = library.progressFor(content.url)
        val progress = if (chunks.isEmpty() || saved >= chunks.size) {
            0
        } else {
            saved.coerceIn(0, chunks.lastIndex)
        }
        library.saveBook(
            url = content.url,
            title = content.title,
            siteName = content.siteName,
            chapterTitle = content.title,
            progress = progress,
            total = chunks.size
        )
        _state.update {
            it.copy(
                reader = content,
                readerLoading = false,
                screen = Screen.READER,
                title = content.title,
                error = null,
                library = library.list()
            )
        }
        if (pendingAutoPlay) {
            pendingAutoPlay = false
            startTts()
        }
    }

    fun closeReader() {
        pendingAutoPlay = false
        _state.update { it.copy(screen = Screen.BROWSER, readerLoading = false) }
    }

    fun nextChapter(auto: Boolean = false) {
        val content = _state.value.reader
        if (content == null) {
            pendingAutoPlay = false
            if (!auto) _state.update { it.copy(error = "Chưa có nội dung đang đọc.") }
            return
        }
        val next = content.nextUrl
        if (next == null) {
            pendingAutoPlay = false
            if (!auto) {
                _state.update { it.copy(error = "Không tìm thấy chương tiếp theo trên trang này.") }
            }
            return
        }
        if (!UrlPolicy.isAllowed(next)) {
            pendingAutoPlay = false
            _state.update { it.copy(error = "Chương tiếp theo dùng URL không an toàn.") }
            return
        }
        if (!chapterGuard.canVisit(next)) {
            pendingAutoPlay = false
            if (!auto) {
                _state.update { it.copy(error = "Chương này đã đọc. Đã dừng để tránh lặp vô hạn.") }
            }
            return
        }
        val view = webView
        if (view == null) {
            pendingAutoPlay = false
            _state.update { it.copy(error = "WebView chưa sẵn sàng.") }
            return
        }
        pendingAutoReader = true
        _state.update {
            it.copy(screen = Screen.BROWSER, readerLoading = true, error = null)
        }
        view.loadUrl(next)
    }

    fun clearChapterHistory() {
        chapterGuard.clear()
    }

    fun chapterHistorySize(): Int = chapterGuard.size()

    // ------------------------------------------------------------------ TTS

    private fun chunksFor(content: ReaderContent): List<String> =
        NativeCore.ttsChunks(content.paragraphs.joinToString("\n\n"), TTS_CHUNK_SIZE)

    fun startTts() {
        val content = _state.value.reader ?: return
        val chunks = chunksFor(content)
        if (chunks.isEmpty()) {
            _state.update { it.copy(error = "Không có văn bản để đọc.") }
            return
        }
        val saved = library.progressFor(content.url)
        val resume = if (saved >= chunks.size) 0 else saved.coerceAtLeast(0)
        PlaybackSession.prepare(
            url = content.url,
            title = content.title,
            chunks = chunks,
            startIndex = resume,
            rate = settings.ttsRate,
            pitch = settings.ttsPitch
        )
        TtsPlaybackService.start(getApplication())
    }

    fun pauseTts() = TtsPlaybackService.pause(getApplication())

    fun stopTts() = TtsPlaybackService.stop(getApplication())

    fun nextTts() = TtsPlaybackService.next(getApplication())

    fun previousTts() = TtsPlaybackService.previous(getApplication())

    fun setTtsRate(value: Float) {
        settings.ttsRate = value
        PlaybackSession.setRate(value)
        if (PlaybackSession.state.value.isPlaying) {
            TtsPlaybackService.updateParams(getApplication())
        }
    }

    fun setTtsPitch(value: Float) {
        settings.ttsPitch = value
        PlaybackSession.setPitch(value)
        if (PlaybackSession.state.value.isPlaying) {
            TtsPlaybackService.updateParams(getApplication())
        }
    }

    // --------------------------------------------------------------- Library

    fun openLibrary() {
        pendingAutoPlay = false
        _state.update { it.copy(screen = Screen.LIBRARY, library = library.list(), error = null) }
    }

    fun openBook(book: BookRecord) {
        _state.update { it.copy(screen = Screen.BROWSER) }
        navigate(book.url, autoReader = true)
    }

    fun deleteBook(book: BookRecord) {
        library.remove(book.url)
        _state.update { it.copy(library = library.list()) }
    }

    fun clearLibrary() {
        library.clear()
        _state.update { it.copy(library = emptyList()) }
    }

    // -------------------------------------------------------------- Settings

    fun openSettings() {
        pendingAutoPlay = false
        _state.update {
            it.copy(
                screen = Screen.SETTINGS,
                adBlockEnabled = settings.adBlockEnabled,
                autoNextChapter = settings.autoNextChapter,
                fontScale = settings.readerFontScale
            )
        }
    }

    fun closeOverlay() {
        _state.update { it.copy(screen = Screen.BROWSER) }
    }

    fun setAdBlockEnabled(enabled: Boolean) {
        settings.adBlockEnabled = enabled
        _state.update { it.copy(adBlockEnabled = enabled) }
        webView?.reload()
    }

    fun toggleAdsForCurrentSite() {
        val host = UrlPolicy.hostOf(_state.value.currentUrl) ?: return
        val allow = !_state.value.adBlockException
        settings.setAdBlockException(host, allow)
        _state.update { it.copy(adBlockException = allow) }
        webView?.reload()
    }

    fun setAutoNextChapter(enabled: Boolean) {
        settings.autoNextChapter = enabled
        _state.update { it.copy(autoNextChapter = enabled) }
    }

    fun setFontScale(value: Float) {
        val scale = value.coerceIn(0.8f, 1.6f)
        settings.readerFontScale = scale
        _state.update { it.copy(fontScale = scale) }
    }

    fun clearAdBlockExceptions() {
        settings.clearAdBlockExceptions()
        _state.update { it.copy(adBlockException = false) }
    }

    // ------------------------------------------------------------ Navigation

    /** Returns false when the Activity should finish. */
    fun handleBack(): Boolean {
        return when (_state.value.screen) {
            Screen.READER -> {
                closeReader()
                true
            }
            Screen.LIBRARY, Screen.SETTINGS -> {
                closeOverlay()
                true
            }
            Screen.BROWSER -> {
                val view = webView
                if (view?.canGoBack() == true) {
                    view.goBack()
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun isAdBlockActive(): Boolean =
        _state.value.adBlockEnabled && !_state.value.adBlockException

    private fun injectCosmeticFilters(view: WebView) {
        val css = JSONObject.quote(AdBlocker.COSMETIC_CSS)
        val script = """
            (function() {
              if (document.getElementById('ur-adblock')) return;
              var style = document.createElement('style');
              style.id = 'ur-adblock';
              style.textContent = $css;
              document.documentElement.appendChild(style);
            })();
        """.trimIndent()
        view.evaluateJavascript(script, null)
    }

    private companion object {
        const val TTS_CHUNK_SIZE = 320
    }
}
