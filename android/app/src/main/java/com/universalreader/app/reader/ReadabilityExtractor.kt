package com.universalreader.app.reader

import android.content.Context
import android.webkit.WebView
import com.universalreader.app.core.NativeCore
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

/**
 * Runs a site adapter or Mozilla Readability inside the already-rendered page.
 *
 * Extraction deliberately stays inside the WebView: no JavaScript-to-native
 * bridge is exposed, so a hostile page cannot call Kotlin/Rust directly.
 */
class ReadabilityExtractor(private val context: Context) {

    private val readabilityJs: String by lazy {
        context.assets.open("readability.js").bufferedReader().use { it.readText() }
    }

    fun extract(
        webView: WebView,
        url: String,
        adapter: SiteAdapter?,
        callback: (ExtractionResult) -> Unit
    ) {
        val adapterLiteral = adapter?.let { adapterJson(it) } ?: "null"
        val script = """
            (function() {
              try {
                var adapter = $adapterLiteral;
                function textOf(el) {
                  return el ? (el.innerText || el.textContent || '') : '';
                }
                function findNext() {
                  var selectors = [
                    'a[rel~="next"]', 'a.next', 'a.next-chapter', '.next a',
                    'a[title*="Next"]', 'a[title*="next"]',
                    'a[aria-label*="Next"]', 'a[aria-label*="next"]'
                  ];
                  for (var i = 0; i < selectors.length; i++) {
                    var candidate = document.querySelector(selectors[i]);
                    if (candidate && candidate.href) return candidate.href;
                  }
                  var links = document.querySelectorAll('a');
                  var pattern = /(next|tiếp theo|chuong sau|chương sau|trang sau|bài sau|phần sau)/i;
                  for (var j = 0; j < links.length; j++) {
                    var label = (links[j].innerText || links[j].textContent || '').trim();
                    if (pattern.test(label) && links[j].href) return links[j].href;
                  }
                  return null;
                }
                if (adapter) {
                  var content = null;
                  for (var c = 0; c < adapter.contentSelectors.length; c++) {
                    var candidate = document.querySelector(adapter.contentSelectors[c]);
                    if (candidate && textOf(candidate).trim().length > 200) {
                      content = candidate;
                      break;
                    }
                  }
                  if (content) {
                    for (var r = 0; r < adapter.removeSelectors.length; r++) {
                      var nodes = content.querySelectorAll(adapter.removeSelectors[r]);
                      for (var n = nodes.length - 1; n >= 0; n--) {
                        if (nodes[n].parentNode) nodes[n].parentNode.removeChild(nodes[n]);
                      }
                    }
                    var titleEl = null;
                    for (var t = 0; t < adapter.titleSelectors.length; t++) {
                      titleEl = document.querySelector(adapter.titleSelectors[t]);
                      if (titleEl) break;
                    }
                    return JSON.stringify({
                      ok: true,
                      source: 'adapter',
                      title: textOf(titleEl).trim() || document.title,
                      textContent: textOf(content),
                      nextUrl: findNext(),
                      byline: null,
                      siteName: adapter.name,
                      lang: document.documentElement.lang || null
                    });
                  }
                }
                var article = new Readability(document.cloneNode(true), { charThreshold: 200 }).parse();
                if (!article || !article.textContent || article.textContent.trim().length < 200) {
                  return JSON.stringify({ ok: false, reason: 'no-article' });
                }
                return JSON.stringify({
                  ok: true,
                  source: 'readability',
                  title: article.title || document.title,
                  textContent: article.textContent,
                  nextUrl: findNext(),
                  byline: article.byline || null,
                  siteName: article.siteName || null,
                  lang: article.lang || null
                });
              } catch (error) {
                return JSON.stringify({ ok: false, reason: String(error) });
              }
            })();
        """.trimIndent()

        webView.evaluateJavascript(readabilityJs + "\n" + script) { raw ->
            callback(parseResult(raw, url))
        }
    }

    /**
     * Fallback when Readability fails: read the user's current text selection.
     */
    fun extractSelection(
        webView: WebView,
        url: String,
        title: String,
        callback: (ExtractionResult) -> Unit
    ) {
        val script = """
            (function() {
              try {
                var text = window.getSelection ? String(window.getSelection()) : '';
                return JSON.stringify({ ok: text.trim().length > 0, text: text });
              } catch (error) {
                return JSON.stringify({ ok: false, reason: String(error) });
              }
            })();
        """.trimIndent()

        webView.evaluateJavascript(script) { raw ->
            val decoded = decodeJsString(raw)
            val json = decoded?.let { runCatching { JSONObject(it) }.getOrNull() }
            if (json == null || !json.optBoolean("ok")) {
                callback(ExtractionResult(error = "Không tìm thấy nội dung. Hãy bôi đen đoạn văn trên trang rồi thử lại."))
                return@evaluateJavascript
            }
            val paragraphs = NativeCore.normalizeParagraphs(json.optString("text"))
            if (paragraphs.isEmpty()) {
                callback(ExtractionResult(error = "Đoạn bôi đen không có văn bản."))
                return@evaluateJavascript
            }
            callback(
                ExtractionResult(
                    content = ReaderContent(
                        url = NativeCore.canonicalizeUrl(url) ?: url,
                        title = title.ifBlank { "Đoạn đã chọn" },
                        paragraphs = paragraphs,
                        source = ExtractionSource.SELECTION
                    )
                )
            )
        }
    }

    private fun parseResult(raw: String?, url: String): ExtractionResult {
        val decoded = decodeJsString(raw)
            ?: return ExtractionResult(error = "Trang không trả về kết quả trích xuất.")
        val json = runCatching { JSONObject(decoded) }.getOrNull()
            ?: return ExtractionResult(error = "Kết quả trích xuất không hợp lệ.")
        if (!json.optBoolean("ok")) {
            val reason = json.optionalString("reason")
            return ExtractionResult(
                error = if (reason == "no-article") {
                    "Không tìm thấy bài viết. Hãy bôi đen đoạn văn và chọn “Đọc đoạn đã chọn”."
                } else {
                    "Không trích xuất được nội dung: ${reason ?: "lỗi không xác định"}"
                }
            )
        }

        val paragraphs = NativeCore.normalizeParagraphs(json.optString("textContent"))
        if (paragraphs.isEmpty()) {
            return ExtractionResult(error = "Trang không có văn bản để đọc.")
        }

        val source = if (json.optString("source") == "adapter") {
            ExtractionSource.SITE_ADAPTER
        } else {
            ExtractionSource.READABILITY
        }
        val rawNext = json.optionalString("nextUrl")
        val nextUrl = rawNext?.let { NativeCore.canonicalizeUrl(it) ?: it }

        return ExtractionResult(
            content = ReaderContent(
                url = NativeCore.canonicalizeUrl(url) ?: url,
                title = json.optionalString("title") ?: "Không tiêu đề",
                byline = json.optionalString("byline"),
                siteName = json.optionalString("siteName"),
                paragraphs = paragraphs,
                nextUrl = nextUrl,
                source = source
            )
        )
    }

    private fun adapterJson(adapter: SiteAdapter): String = JSONObject().apply {
        put("name", adapter.name)
        put("contentSelectors", JSONArray(adapter.contentSelectors))
        put("titleSelectors", JSONArray(adapter.titleSelectors))
        put("removeSelectors", JSONArray(adapter.removeSelectors))
    }.toString()

    private fun decodeJsString(raw: String?): String? {
        if (raw == null || raw == "null") return null
        return runCatching { JSONTokener(raw).nextValue() as? String }.getOrNull()
    }

    private fun JSONObject.optionalString(key: String): String? {
        if (isNull(key)) return null
        return optString(key).takeIf { it.isNotBlank() && it != "null" }
    }
}
