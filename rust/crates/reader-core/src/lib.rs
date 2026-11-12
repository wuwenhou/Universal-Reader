//! Portable reader domain utilities. No Android dependencies.
//!
//! The only Android-specific code lives in [`ffi`], which is a thin JNI
//! marshalling layer over the functions defined here.

mod ffi;

use std::collections::HashSet;
use url::Url;

/// Native ABI version exposed to Kotlin through `NativeBridge.coreVersion()`.
///
/// Bump this whenever the JNI surface changes.
pub const CORE_VERSION: u32 = 2;

/// Returns the native ABI version. Kept separate from the JNI wrapper so it
/// can be unit-tested on the host.
pub fn core_version() -> u32 {
    CORE_VERSION
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Chapter {
    pub title: String,
    pub paragraphs: Vec<String>,
    pub next_url: Option<String>,
}

/// Splits text into paragraphs on blank lines and collapses whitespace inside
/// each paragraph. Readability output uses blank lines between blocks, while
/// hard-wrapped HTML text may use single newlines that belong to one paragraph.
pub fn normalize_paragraphs(text: &str) -> Vec<String> {
    let mut paragraphs = Vec::new();
    let mut current = String::new();

    for raw_line in text.lines() {
        let line = raw_line.split_whitespace().collect::<Vec<_>>().join(" ");
        if line.is_empty() {
            flush_paragraph(&mut paragraphs, &mut current);
        } else {
            if !current.is_empty() {
                current.push(' ');
            }
            current.push_str(&line);
        }
    }
    flush_paragraph(&mut paragraphs, &mut current);
    paragraphs
}

fn flush_paragraph(paragraphs: &mut Vec<String>, current: &mut String) {
    let trimmed = current.trim();
    if !trimmed.is_empty() {
        paragraphs.push(trimmed.to_owned());
    }
    current.clear();
}

/// Splits text into TTS-sized chunks at UTF-8 boundaries.
///
/// The splitter prefers sentence-ending punctuation (including Vietnamese and
/// CJK marks), then whitespace, and only falls back to a hard cut when a
/// sentence is longer than `max_chars`.
pub fn tts_chunks(text: &str, max_chars: usize) -> Vec<String> {
    if max_chars == 0 {
        return Vec::new();
    }

    let chars: Vec<char> = text.chars().collect();
    let mut chunks = Vec::new();
    let mut start = 0;

    while start < chars.len() {
        let hard_end = (start + max_chars).min(chars.len());
        let end = if hard_end == chars.len() {
            hard_end
        } else {
            let sentence_end = (start + 1..=hard_end).rev().find(|&i| {
                matches!(
                    chars[i - 1],
                    '.' | '!' | '?' | '…' | '。' | '！' | '？' | ';' | ':'
                )
            });
            sentence_end
                .or_else(|| {
                    (start + 1..=hard_end)
                        .rev()
                        .find(|&i| chars[i - 1].is_whitespace())
                })
                .unwrap_or(hard_end)
        };

        let chunk: String = chars[start..end].iter().collect();
        let trimmed = chunk.trim();
        if !trimmed.is_empty() {
            chunks.push(trimmed.to_owned());
        }

        // `end` is always at least `start + 1`, but guard against pathological
        // input anyway so the loop can never spin forever.
        start = end.max(start + 1);
    }

    chunks
}

/// Query parameters that only track users and never change the document.
const TRACKING_PARAMS: &[&str] = &[
    "fbclid", "gclid", "dclid", "yclid", "msclkid", "mc_cid", "mc_eid", "igshid", "mkt_tok",
];

fn is_tracking_param(key: &str) -> bool {
    key.starts_with("utm_") || TRACKING_PARAMS.contains(&key)
}

/// Canonicalizes an http(s) URL for deduplication and library keys.
///
/// - lowercases the scheme and host (the `url` crate does this),
/// - removes the fragment,
/// - removes tracking parameters,
/// - sorts remaining query parameters,
/// - rejects every non-http(s) scheme.
pub fn canonicalize_url(input: &str) -> Option<String> {
    let trimmed = input.trim();
    if trimmed.is_empty() {
        return None;
    }

    let mut url = Url::parse(trimmed).ok()?;
    if !matches!(url.scheme(), "http" | "https") {
        return None;
    }

    url.set_fragment(None);

    let mut pairs: Vec<(String, String)> = url
        .query_pairs()
        .filter(|(key, _)| {
            let key = key.to_ascii_lowercase();
            !is_tracking_param(&key)
        })
        .map(|(key, value)| (key.into_owned(), value.into_owned()))
        .collect();

    if pairs.is_empty() {
        url.set_query(None);
    } else {
        pairs.sort();
        let mut query = url.query_pairs_mut();
        query.clear();
        for (key, value) in &pairs {
            query.append_pair(key, value);
        }
    }

    Some(url.to_string())
}

/// Remembers canonical chapter URLs so automatic next-chapter navigation can
/// never loop forever.
#[derive(Debug, Default)]
pub struct ChapterNavigator {
    visited: HashSet<String>,
}

impl ChapterNavigator {
    /// Marks a URL as visited. Returns `true` when the URL was not seen before.
    pub fn mark_visited(&mut self, url: &str) -> bool {
        match canonicalize_url(url) {
            Some(key) => self.visited.insert(key),
            None => false,
        }
    }

    pub fn can_visit(&self, url: &str) -> bool {
        match canonicalize_url(url) {
            Some(key) => !self.visited.contains(&key),
            None => false,
        }
    }

    pub fn is_visited(&self, url: &str) -> bool {
        match canonicalize_url(url) {
            Some(key) => self.visited.contains(&key),
            None => false,
        }
    }

    pub fn clear(&mut self) {
        self.visited.clear();
    }
}

/// A simple ABI smoke-test function for Android JNI wiring.
#[no_mangle]
pub extern "C" fn universal_reader_core_version() -> u32 {
    CORE_VERSION
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn vietnamese_text_is_preserved() {
        assert_eq!(
            normalize_paragraphs("Xin   chào\n\nThế giới!"),
            vec!["Xin chào", "Thế giới!"]
        );
    }

    #[test]
    fn paragraphs_split_on_blank_lines_and_join_hard_wraps() {
        assert_eq!(
            normalize_paragraphs("Dòng một\nvẫn cùng đoạn\n\nĐoạn hai\n\n\nĐoạn ba"),
            vec!["Dòng một vẫn cùng đoạn", "Đoạn hai", "Đoạn ba"]
        );
    }

    #[test]
    fn chunking_preserves_unicode() {
        let source = "Xin chào. Đây là chương mới!";
        assert_eq!(
            tts_chunks(source, 10).concat().replace(' ', ""),
            source.replace(' ', "")
        );
    }

    #[test]
    fn chunking_respects_max_chars_and_sentence_boundaries() {
        let source = "Câu một rất dài. Câu hai ngắn! Câu ba?";
        let chunks = tts_chunks(source, 20);
        assert!(chunks.iter().all(|chunk| chunk.chars().count() <= 20));
        assert_eq!(chunks.first().map(String::as_str), Some("Câu một rất dài."));
    }

    #[test]
    fn chunking_handles_zero_and_empty_input() {
        assert!(tts_chunks("", 10).is_empty());
        assert!(tts_chunks("abc", 0).is_empty());
    }

    #[test]
    fn canonicalization_strips_tracking_and_sorts_query() {
        assert_eq!(
            canonicalize_url("HTTPS://Example.ORG:443/a?utm_source=x&b=2&a=1#frag"),
            Some("https://example.org/a?a=1&b=2".to_owned())
        );
    }

    #[test]
    fn canonicalization_rejects_dangerous_schemes() {
        assert_eq!(canonicalize_url("javascript:alert(1)"), None);
        assert_eq!(canonicalize_url("file:///etc/passwd"), None);
        assert_eq!(canonicalize_url("data:text/html,hi"), None);
        assert_eq!(canonicalize_url("not a url"), None);
    }

    #[test]
    fn navigation_blocks_cycles_after_canonicalization() {
        let mut nav = ChapterNavigator::default();
        assert!(nav.mark_visited("https://example.org/ch1?utm_source=feed"));
        assert!(!nav.can_visit("https://example.org/ch1#top"));
        assert!(!nav.mark_visited("https://example.org/ch1"));
        assert!(nav.can_visit("https://example.org/ch2"));
        assert!(!nav.can_visit("javascript:alert(1)"));
    }

    #[test]
    fn core_version_is_stable() {
        assert_eq!(core_version(), 2);
    }
}
