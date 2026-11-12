# Changelog

All notable changes to Universal Reader are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and
versions use [Semantic Versioning](https://semver.org/).

## [0.2.0] — 2026-11-12

Preview release covering M2–M6. This is still a test build, not a Play Store
production release.

### Added

- Reader mode using Mozilla Readability.js inside the WebView, with 45 curated
  site adapter hints and manual text-selection fallback.
- HTTPS-only URL policy that rejects `http`, `javascript:`, `file:`, `content:`,
  `data:` and `intent:` URLs.
- Hardened WebView configuration: no JavaScript bridge, no file/content access,
  no mixed content, SSL errors always cancelled, Safe Browsing enabled.
- Vietnamese TTS with sentence queue, speed/pitch controls, saved chunk progress,
  foreground `mediaPlayback` service, platform MediaSession, audio focus handling,
  headphone-unplug pause, wake lock and notification controls.
- Automatic next-chapter detection and optional auto-play with canonical-URL loop
  prevention.
- Local library with book/chapter metadata and reading progress.
- Conservative ad/tracker filtering with a global switch and per-site exceptions.
- Rust core v2: `normalizeParagraphs`, `ttsChunks`, `canonicalizeUrl`,
  `ChapterNavigator`, and 9 unit tests.
- 27 JVM unit tests for URL policy, ad filtering, library codec, playback queue,
  chapter guard and site adapter registry.
- Adaptive launcher icon, backup/data-extraction rules, release signing through
  `UR_KEYSTORE`/`UR_KEYSTORE_PASSWORD`/`UR_KEY_ALIAS`/`UR_KEY_PASSWORD`.
- `CHANGELOG.md` and `SECURITY.md`.

### Changed

- `versionCode` 2, `versionName` 0.2.0.
- R8/minify and resource shrinking enabled for release builds, with JNI keep rules.
- WebView user agent now includes the app version.
- CI runs unit tests, builds debug and release APKs, and verifies all four JNI
  symbols plus 16 KB page alignment.
- `verify-apk.ps1` and `build-native.ps1` now check the complete JNI surface.

### Fixed

- TTS now registers an `UtteranceProgressListener`, so playback advances past the
  first sentence.
- Transient audio-focus loss keeps the focus request, allowing playback to resume
  when focus returns.
- A stale TTS notification action can no longer keep an empty foreground service
  alive.
- Library progress resets when a previously completed chapter is reopened.

### Known limitations

- No device/emulator smoke test was possible in this environment; screen-off
  playback and audio-focus interruptions still need hardware verification.
- Cloud TTS, audio caching, multi-chapter prefetch, Room/SQLite and a full filter
  list are not implemented.
- The release signing key is generated outside the repository; back it up before
  distributing production builds.

## [0.1.0] — 2026-11-12

### Added

- Initial Kotlin/Compose Android scaffold and portable Rust `reader-core`.
- JNI smoke test: `NativeBridge.coreVersion()` returns 1.
- Debug APK for `arm64-v8a` and `x86_64`, 16 KB page alignment verification.
- Rust fmt/clippy/test gates and GitHub Actions CI.
- Build scripts, toolchain documentation and implementation plan.
