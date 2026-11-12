package com.universalreader.app.reader

import java.net.URI

/**
 * Per-site extraction hints.
 *
 * Adapters are only hints: if the configured container is missing or too
 * short, [ReadabilityExtractor] falls back to Mozilla Readability. This keeps
 * the app working when a site changes its DOM.
 */
data class SiteAdapter(
    val id: String,
    val name: String,
    val hosts: List<String>,
    val contentSelectors: List<String>,
    val titleSelectors: List<String> = emptyList(),
    val removeSelectors: List<String> = emptyList()
) {
    fun matches(host: String): Boolean =
        hosts.any { host == it || host.endsWith(".$it") }
}

object SiteAdapterRegistry {
    /**
     * Curated list of article/novel hosts. Selectors are intentionally simple
     * and are validated by `SiteAdapterRegistryTest` against a synthetic DOM.
     */
    val adapters: List<SiteAdapter> = listOf(
        SiteAdapter(
            "wikipedia", "Wikipedia", listOf("wikipedia.org", "wikisource.org"),
            listOf("#mw-content-text .mw-parser-output", "#mw-content-text", "#bodyContent"),
            listOf("#firstHeading"),
            listOf(".navbox", ".infobox", ".mw-editsection", ".reference", ".metadata")
        ),
        SiteAdapter(
            "medium", "Medium", listOf("medium.com"),
            listOf("article"), listOf("h1"),
            listOf(".meteredContent", ".paywall", ".js-postShareWidget")
        ),
        SiteAdapter(
            "devto", "DEV Community", listOf("dev.to"),
            listOf("#article-body", "article"), listOf("h1"),
            listOf(".highlight__panel", ".print-hidden")
        ),
        SiteAdapter(
            "substack", "Substack", listOf("substack.com"),
            listOf(".available-content", ".body.markup", "article"), listOf("h1")
        ),
        SiteAdapter(
            "wordpress", "WordPress", listOf("wordpress.com"),
            listOf(".entry-content", "article"), listOf(".entry-title", "h1"),
            listOf(".sharedaddy", ".jp-relatedposts", ".wpcnt")
        ),
        SiteAdapter(
            "blogspot", "Blogger", listOf("blogspot.com"),
            listOf(".post-body", "article"), listOf(".post-title", "h1")
        ),
        SiteAdapter(
            "nytimes", "The New York Times", listOf("nytimes.com"),
            listOf("section[name=\"articleBody\"]", "article"), listOf("h1"),
            listOf("[data-testid=\"inline-message\"]", ".css-1fanzo5")
        ),
        SiteAdapter(
            "guardian", "The Guardian", listOf("theguardian.com"),
            listOf("[data-guide=\"article-body\"]", ".article-body-commercial-selector", "article"),
            listOf("h1")
        ),
        SiteAdapter(
            "bbc", "BBC", listOf("bbc.com", "bbc.co.uk"),
            listOf("article", "[data-component=\"text-block\"]"), listOf("h1")
        ),
        SiteAdapter(
            "reuters", "Reuters", listOf("reuters.com"),
            listOf("article", "[data-testid=\"paragraph-0\"]"), listOf("h1")
        ),
        SiteAdapter(
            "apnews", "AP News", listOf("apnews.com"),
            listOf(".RichTextStoryBody", ".Article", "article"), listOf(".Page-headline", "h1")
        ),
        SiteAdapter(
            "cnn", "CNN", listOf("cnn.com"),
            listOf(".article__content", "article"), listOf(".article__title", "h1")
        ),
        SiteAdapter(
            "vnexpress", "VnExpress", listOf("vnexpress.net"),
            listOf(".fck_detail", "article"), listOf("h1.title-detail", "h1"),
            listOf(".box-tinlienquan", ".relate")
        ),
        SiteAdapter(
            "tuoitre", "Tuổi Trẻ", listOf("tuoitre.vn"),
            listOf(".detail-content", "article"), listOf(".detail-title", "h1"),
            listOf(".box-related", ".tin-lien-quan")
        ),
        SiteAdapter(
            "thanhnien", "Thanh Niên", listOf("thanhnien.vn"),
            listOf(".detail-content", "article"), listOf(".detail-title", "h1")
        ),
        SiteAdapter(
            "dantri", "Dân Trí", listOf("dantri.com.vn"),
            listOf(".singular-content", ".dt-news__content", "article"),
            listOf(".title-page", "h1")
        ),
        SiteAdapter(
            "vietnamnet", "VietNamNet", listOf("vietnamnet.vn"),
            listOf(".maincontent", ".content", "article"),
            listOf(".content-detail-title", "h1")
        ),
        SiteAdapter(
            "zingnews", "Zing News", listOf("zingnews.vn"),
            listOf(".the-article-body", "article"), listOf(".the-article-title", "h1")
        ),
        SiteAdapter(
            "nhandan", "Nhân Dân", listOf("nhandan.vn"),
            listOf(".detail__content", "article"), listOf(".detail__title", "h1")
        ),
        SiteAdapter(
            "laodong", "Lao Động", listOf("laodong.vn"),
            listOf(".detail-content", "article"), listOf(".detail-title", "h1")
        ),
        SiteAdapter(
            "vov", "VOV", listOf("vov.vn"),
            listOf(".article-content", ".content", "article"), listOf(".article-title", "h1")
        ),
        SiteAdapter(
            "qq", "Tencent News", listOf("qq.com"),
            listOf("#Cnt-Main-Article-QQ", ".content-article", "article"),
            listOf(".tit", "h1")
        ),
        SiteAdapter(
            "sina", "Sina", listOf("sina.com.cn"),
            listOf("#artibody", ".article", "article"), listOf(".main-title", "h1")
        ),
        SiteAdapter(
            "sohu", "Sohu", listOf("sohu.com"),
            listOf("#mp-editor", ".article", "article"), listOf(".text-title", "h1")
        ),
        SiteAdapter(
            "netease", "NetEase", listOf("163.com"),
            listOf("#endText", ".post_body", "article"), listOf(".post_title", "h1")
        ),
        SiteAdapter(
            "asahi", "Asahi Shimbun", listOf("asahi.com"),
            listOf("article", ".nfyQ"), listOf("h1")
        ),
        SiteAdapter(
            "yomiuri", "Yomiuri Shimbun", listOf("yomiuri.co.jp"),
            listOf(".main-content", "article"), listOf("h1")
        ),
        SiteAdapter(
            "lemonde", "Le Monde", listOf("lemonde.fr"),
            listOf(".article__content", "article"), listOf("h1")
        ),
        SiteAdapter(
            "elpais", "El País", listOf("elpais.com"),
            listOf(".a_c", "article"), listOf("h1")
        ),
        SiteAdapter(
            "spiegel", "Der Spiegel", listOf("spiegel.de"),
            listOf("[data-area=\"article-body\"]", "article"), listOf("h1")
        ),
        SiteAdapter(
            "heise", "heise online", listOf("heise.de"),
            listOf(".article-content", "#meldung", "article"), listOf("h1")
        ),
        SiteAdapter(
            "arstechnica", "Ars Technica", listOf("arstechnica.com"),
            listOf(".article-content", "article"), listOf("h1")
        ),
        SiteAdapter(
            "theverge", "The Verge", listOf("theverge.com"),
            listOf(".duet--article--article-body-component", "article"), listOf("h1")
        ),
        SiteAdapter(
            "wired", "Wired", listOf("wired.com"),
            listOf(".body__inner-container", "article"), listOf("h1")
        ),
        SiteAdapter(
            "techcrunch", "TechCrunch", listOf("techcrunch.com"),
            listOf(".article-content", ".entry-content", "article"), listOf("h1")
        ),
        SiteAdapter(
            "royalroad", "Royal Road", listOf("royalroad.com"),
            listOf(".chapter-content", ".chapter-inner"), listOf(".fic-header h1", "h1"),
            listOf(".author-note-portlet")
        ),
        SiteAdapter(
            "scribblehub", "Scribble Hub", listOf("scribblehub.com"),
            listOf(".chp_raw", "#chp_raw"), listOf(".chapter-title", "h1")
        ),
        SiteAdapter(
            "wattpad", "Wattpad", listOf("wattpad.com"),
            listOf(".story-part", "article"), listOf("h1")
        ),
        SiteAdapter(
            "fanfiction", "FanFiction.net", listOf("fanfiction.net"),
            listOf("#storytext"), listOf("#profile_top b", "h1")
        ),
        SiteAdapter(
            "ao3", "Archive of Our Own", listOf("archiveofourown.org"),
            listOf("#chapters .userstuff", "#workskin", "article"), listOf(".title", "h1")
        ),
        SiteAdapter(
            "truyenfull", "Truyện Full", listOf("truyenfull.vn", "truyenfull.com"),
            listOf(".chapter-c", "#chapter-c"), listOf(".chapter-title", "h1")
        ),
        SiteAdapter(
            "metruyencv", "Metruyencv", listOf("metruyencv.com"),
            listOf(".nh-read__content", "#js-read__content", "article"), listOf("h1")
        ),
        SiteAdapter(
            "tangthuvien", "Tàng Thư Viện", listOf("tangthuvien.vn"),
            listOf("#content", ".noidung", "article"), listOf("h1")
        ),
        SiteAdapter(
            "gutenberg", "Project Gutenberg", listOf("gutenberg.org"),
            listOf(".chapter", "#pg-machine-header", "body"), listOf("h1", "h2")
        ),
        SiteAdapter(
            "standardebooks", "Standard Ebooks", listOf("standardebooks.org"),
            listOf(".chapter", "section", "article"), listOf("h1")
        ),
        SiteAdapter(
            "githubblog", "GitHub Blog", listOf("github.blog"),
            listOf(".post-content", "article"), listOf("h1")
        )
    )

    fun forUrl(url: String): SiteAdapter? {
        val host = runCatching { URI(url).host?.lowercase() }.getOrNull() ?: return null
        return adapters.firstOrNull { it.matches(host) }
    }

    fun byId(id: String): SiteAdapter? = adapters.firstOrNull { it.id == id }
}
