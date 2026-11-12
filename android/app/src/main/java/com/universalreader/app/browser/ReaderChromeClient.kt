package com.universalreader.app.browser

import android.webkit.WebChromeClient
import android.webkit.WebView

/** Keeps page progress and titles out of the Activity/Compose layer. */
class ReaderChromeClient(
    private val onProgress: (Int) -> Unit,
    private val onTitle: (String) -> Unit
) : WebChromeClient() {
    override fun onProgressChanged(view: WebView, newProgress: Int) {
        onProgress(newProgress)
    }

    override fun onReceivedTitle(view: WebView, title: String?) {
        if (!title.isNullOrBlank()) onTitle(title)
    }
}
