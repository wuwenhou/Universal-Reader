package com.universalreader.app.net

import java.net.URI

/**
 * Small, self-authored network filter.
 *
 * This is deliberately not a general-purpose ad blocker: it blocks a curated
 * list of well-known ad/tracking hosts and hides common ad containers. It does
 * not download third-party filter lists, so there is no external license or
 * attribution obligation. A per-site exception is handled by the caller.
 */
object AdBlocker {
    private val blockedHosts = setOf(
        "2mdn.net",
        "3lift.com",
        "adnxs.com",
        "adsrvr.org",
        "adsterra.com",
        "amazon-adsystem.com",
        "casalemedia.com",
        "criteo.com",
        "criteo.net",
        "doubleclick.net",
        "googleadservices.com",
        "googlesyndication.com",
        "google-analytics.com",
        "googletagmanager.com",
        "hotjar.com",
        "mgid.com",
        "moatads.com",
        "openx.net",
        "outbrain.com",
        "popads.net",
        "propellerads.com",
        "pubmatic.com",
        "quantserve.com",
        "revcontent.com",
        "rubiconproject.com",
        "scorecardresearch.com",
        "sharethrough.com",
        "smartadserver.com",
        "taboola.com",
        "teads.tv",
        "zedo.com"
    )

    private val blockedPathFragments = listOf(
        "/ads/",
        "/adserver/",
        "/analytics.js",
        "/gtag/js",
        "/pagead/",
        "/pixel?",
        "/tracking/"
    )

    fun shouldBlock(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme != "http" && scheme != "https") return false

        val host = uri.host?.lowercase() ?: return false
        if (blockedHosts.any { host == it || host.endsWith(".$it") }) return true

        val path = (uri.rawPath ?: "") + (uri.rawQuery?.let { "?$it" } ?: "")
        return blockedPathFragments.any { path.contains(it, ignoreCase = true) }
    }

    /**
     * Conservative cosmetic rules. Avoid broad selectors such as `[class*=ad]`
     * because they can hide legitimate layout elements.
     */
    const val COSMETIC_CSS: String = """
        ins.adsbygoogle,
        .adsbygoogle,
        .advertisement,
        .ad-slot,
        .ad-container,
        .ad-wrapper,
        .sponsored-post,
        [id^="google_ads"],
        [id^="div-gpt-ad"],
        [data-ad-slot],
        [data-ad-client] { display: none !important; }
    """
}
