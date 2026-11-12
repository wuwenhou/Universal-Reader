package com.universalreader.app.net

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdBlockerTest {

    @Test
    fun blocksKnownAdHostsAndSubdomains() {
        assertTrue(AdBlocker.shouldBlock("https://doubleclick.net/pixel"))
        assertTrue(AdBlocker.shouldBlock("https://ads.doubleclick.net/pixel"))
        assertTrue(AdBlocker.shouldBlock("https://www.google-analytics.com/analytics.js"))
        assertTrue(AdBlocker.shouldBlock("https://cdn.taboola.com/libtrc/x.js"))
    }

    @Test
    fun blocksTrackingPathsOnNormalHosts() {
        assertTrue(AdBlocker.shouldBlock("https://example.org/ads/banner.png"))
        assertTrue(AdBlocker.shouldBlock("https://example.org/tracking/pixel.gif"))
        assertTrue(AdBlocker.shouldBlock("https://example.org/gtag/js?id=G-1"))
    }

    @Test
    fun leavesArticleResourcesAlone() {
        assertFalse(AdBlocker.shouldBlock("https://example.org/article"))
        assertFalse(AdBlocker.shouldBlock("https://example.org/images/hero.jpg"))
        assertFalse(AdBlocker.shouldBlock("https://cdn.example.org/app.js"))
        assertFalse(AdBlocker.shouldBlock("https://example.org/read?pixel=1"))
    }

    @Test
    fun ignoresNonHttpAndMalformedUrls() {
        assertFalse(AdBlocker.shouldBlock("javascript:alert(1)"))
        assertFalse(AdBlocker.shouldBlock("data:text/html,hello"))
        assertFalse(AdBlocker.shouldBlock("not a url"))
        assertFalse(AdBlocker.shouldBlock(""))
    }

    @Test
    fun cosmeticRulesStayNarrow() {
        assertTrue(AdBlocker.COSMETIC_CSS.contains("ins.adsbygoogle"))
        assertFalse(AdBlocker.COSMETIC_CSS.contains("[class*=\"ad\"]"))
    }
}
