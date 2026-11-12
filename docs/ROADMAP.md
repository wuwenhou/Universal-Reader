# Universal Reader — Implementation Roadmap

> Maintained plan and build standards: [../PLAN.md](../PLAN.md), [../BUILD.md](../BUILD.md).
> Release history: [../CHANGELOG.md](../CHANGELOG.md).

## Current status (0.2.0, 2026-11-12)

- [x] Rust workspace and portable text/URL/chapter library (9/9 tests pass)
- [x] Android Gradle project scaffold
- [x] Compose WebView browser with HTTPS-only URL policy and hardened settings
- [x] Readability.js reader mode + 45 per-site adapter hints + manual selection fallback
- [x] Android TTS queue, Vietnamese voice selection, foreground mediaPlayback service,
      platform MediaSession, audio focus, wake lock, notification controls
- [x] Automatic next-chapter detection, loop prevention and auto-play
- [x] Local library and progress (SharedPreferences + JSON codec)
- [x] Conservative ad blocking with per-site exceptions
- [x] 27 JVM unit tests, lint, R8-minified release build
- [x] CI: Rust gates + Android unit tests + debug/release APK + JNI/16 KB verification
- [ ] Device tests (no adb device available yet)
- [ ] Cloud TTS provider with user-managed credentials
- [ ] Offline TTS model evaluation (Piper/Sherpa-onnx)
- [ ] Room/SQLite library migration
- [ ] Instrumented tests on API 26/31/34/35

## Milestones and gates

### M1 Foundation

Rust core compiles and tests pass; Android debug APK builds; JNI smoke test returns
version 2 through `NativeBridge.coreVersion()`.

### M2 Browser and Reader

HTTPS-only navigation; no JS→native bridge; SSL errors never bypassed; Readability
extraction with adapter hints and manual selection fallback; chapter links and loop
guard. Unit tests cover URL policy and adapter registry. Device rotation/back/forward
testing is still pending.

### M3 TTS and playback

Device TTS voice enumeration, offline detection, sentence queue, speed/pitch, resume
state, foreground `mediaPlayback` service, platform MediaSession, audio focus and
headphone-unplug handling. Decision: direct TTS + MediaSession instead of Media3
(see PLAN ADR-007). Screen-off/30-minute device test is still pending.

### M4 Cloud, offline, chapters

Next-chapter detection and automatic playback are implemented with canonical-URL loop
prevention. Cloud TTS with user-managed credentials, quotas, audio caching and
multi-chapter prefetch remain future work.

### M5 Adblock and library

The MVP uses a small self-authored host/path filter plus narrow cosmetic CSS and
per-site exceptions, avoiding third-party filter-list licensing. Library/progress use
SharedPreferences + JSON; Room and migration tests remain future work.

### M6 Release

R8/minify, resource shrinking, JNI keep rules, adaptive icon, release signing via
environment variables, changelog and security notes are in place. CI verifies JNI
symbols and 16 KB alignment. Instrumented and screen-off tests require a device.

## Architecture decisions

- Kotlin owns Android UI, WebView, TTS and playback notifications.
- Rust owns URL canonicalization, chapter loop prevention and text normalization/chunking.
- JNI is a thin, versioned boundary (`CORE_VERSION = 2`); no JavaScript bridge.
- No claims of universal extraction or 100% ad blocking.
- No scraping of protected or access-restricted content.

## Known limitations

- No hardware/emulator smoke test has been run for 0.2.0.
- Cloud TTS, audio caching and multi-chapter prefetch are not implemented.
- Ad filtering is conservative and not a full filter-list engine.
- The library is not backed by Room/SQLite yet.
- `targetSdk` remains 35 because AGP 8.7.3 supports compileSdk 35.
