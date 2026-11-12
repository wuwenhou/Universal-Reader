package com.universalreader.app.browser

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import com.universalreader.app.BuildConfig

/**
 * Hardened WebView defaults.
 *
 * JavaScript is required by most sites and by Readability, but there is no
 * `addJavascriptInterface` bridge, file access is off, mixed content is never
 * allowed, and Safe Browsing stays enabled.
 */
object WebViewConfig {

    @SuppressLint("SetJavaScriptEnabled")
    fun apply(webView: WebView) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true

            allowFileAccess = false
            allowContentAccess = false
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = false
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = false

            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            mediaPlaybackRequiresUserGesture = true
            cacheMode = WebSettings.LOAD_DEFAULT
            setSafeBrowsingEnabled(true)
            userAgentString = "$userAgentString UniversalReader/${BuildConfig.VERSION_NAME}"
        }

        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false)
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
    }
}
