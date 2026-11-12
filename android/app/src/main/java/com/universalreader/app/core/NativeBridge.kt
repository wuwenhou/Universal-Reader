package com.universalreader.app.core

/**
 * Thin JNI boundary to the Rust `reader-core` library.
 *
 * Keep this object limited to marshalling: every real operation belongs in
 * Rust so it can be unit-tested on the host. The native library is built for
 * `arm64-v8a` and `x86_64` by `tools/build-native.ps1`.
 */
object NativeBridge {
    init {
        System.loadLibrary("reader_core")
    }

    /** Native ABI version. Must match `CORE_VERSION` in `reader-core`. */
    external fun coreVersion(): Int

    /** Splits text into paragraphs on blank lines. */
    external fun normalizeParagraphs(text: String): Array<String>

    /** Splits text into sentence-sized chunks for TTS. */
    external fun ttsChunks(text: String, maxChars: Int): Array<String>

    /** Returns a canonical http(s) URL, or null for invalid/unsupported URLs. */
    external fun canonicalizeUrl(url: String): String?
}
