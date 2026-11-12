package com.universalreader.app.browser

import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream

/**
 * WebViewClient that enforces [UrlPolicy] and the network ad filter.
 *
 * SSL errors are always cancelled: the app never bypasses certificate checks.
 */
class ReaderWebViewClient(private val callbacks: Callbacks) : WebViewClient() {

    interface Callbacks {
        fun onPageStarted(url: String)
        fun onPageFinished(url: String)
        fun onUrlChanged(url: String)
        fun onPageError(message: String)
        fun onBlockedUrl(url: String)
        fun shouldBlockRequest(url: String): Boolean
    }

    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest
    ): Boolean {
        val url = request.url.toString()
        if (UrlPolicy.isAllowed(url)) return false
        callbacks.onBlockedUrl(url)
        return true
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        if (url != null) callbacks.onPageStarted(url)
    }

    override fun onPageFinished(view: WebView, url: String?) {
        if (url != null) callbacks.onPageFinished(url)
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
        if (url != null) callbacks.onUrlChanged(url)
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError
    ) {
        if (request.isForMainFrame) {
            callbacks.onPageError("Không tải được trang: ${error.description}")
        }
    }

    override fun onReceivedSslError(
        view: WebView,
        handler: android.webkit.SslErrorHandler,
        error: android.net.http.SslError
    ) {
        handler.cancel()
        callbacks.onPageError("Kết nối không an toàn (chứng chỉ SSL không hợp lệ). Đã chặn trang.")
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        if (request.isForMainFrame) return null
        if (!callbacks.shouldBlockRequest(request.url.toString())) return null
        return WebResourceResponse(
            "text/plain",
            "utf-8",
            ByteArrayInputStream(ByteArray(0))
        )
    }
}
