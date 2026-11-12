//! Portable reader domain utilities. No Android dependencies.
//!
//! The only Android-specific code lives in [`ffi`], which is a thin JNI
//! marshalling layer over the functions defined here.
mod ffi;

use std::collections::HashSet;

/// Native ABI version exposed to Kotlin through `NativeBridge.coreVersion()`.
///
/// Bump this whenever the JNI surface changes.
pub const CORE_VERSION: u32 = 1;

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

/// Cleans plain-text paragraphs for TTS without modifying their meaning.
pub fn normalize_paragraphs(text: &str) -> Vec<String> {
    text.lines()
        .map(|line| line.split_whitespace().collect::<Vec<_>>().join(" "))
        .filter(|line| !line.is_empty())
        .collect()
}

/// Splits text at UTF-8 boundaries; prefers sentence-ending punctuation.
pub fn tts_chunks(text: &str, max_chars: usize) -> Vec<String> {
    if max_chars == 0 {
        return vec![];
    }
    let chars: Vec<char> = text.chars().collect();
    let mut chunks = Vec::new();
    let mut start = 0;
    while start < chars.len() {
        let end = (start + max_chars).min(chars.len());
        let boundary = if end == chars.len() {
            end
        } else {
            (start + 1..=end)
                .rev()
                .find(|&i| matches!(chars[i - 1], '.' | '!' | '?' | '。' | '！' | '？' | '\n'))
                .unwrap_or(end)
        };
        let chunk: String = chars[start..boundary].iter().collect();
        let chunk = chunk.trim();
        if !chunk.is_empty() {
            chunks.push(chunk.to_owned());
        }
        start = boundary;
    }
    chunks
}

#[derive(Debug, Default)]
pub struct ChapterNavigator {
    visited: HashSet<String>,
}

impl ChapterNavigator {
    pub fn mark_visited(&mut self, url: &str) -> bool {
        if !(url.starts_with("https://") || url.starts_with("http://")) {
            return false;
        }
        self.visited.insert(url.to_owned())
    }
    pub fn can_visit(&self, url: &str) -> bool {
        (url.starts_with("https://") || url.starts_with("http://")) && !self.visited.contains(url)
    }
}

/// A simple ABI smoke-test function for Android JNI wiring.
#[no_mangle]
pub extern "C" fn universal_reader_core_version() -> u32 {
    1
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
    fn chunking_preserves_unicode() {
        let source = "Xin chào. Đây là chương mới!";
        assert_eq!(
            tts_chunks(source, 10).concat().replace(' ', ""),
            source.replace(' ', "")
        );
    }
    #[test]
    fn navigation_blocks_cycles() {
        let mut nav = ChapterNavigator::default();
        assert!(nav.mark_visited("https://example.org/ch1"));
        assert!(!nav.can_visit("https://example.org/ch1"));
        assert!(!nav.can_visit("javascript:alert(1)"));
    }
    #[test]
    fn core_version_is_stable() {
        assert_eq!(core_version(), 1);
    }
}
