# Universal Reader — Implementation Roadmap

> Maintained plan and build standards: [../PLAN.md](../PLAN.md), [../BUILD.md](../BUILD.md).

## Current status

- [x] Rust workspace and portable text-processing library
- [x] Rust unit tests written and executed (4/4 pass on 2026-11-12)
- [x] Android Gradle project scaffold
- [x] Basic Compose WebView and plain-text page extraction prototype
- [x] Build Android debug APK (8.9 MB, arm64-v8a + x86_64)
- [ ] Run device tests (no adb device available yet)
- [x] Connect Rust core to Android using JNI (`NativeBridge.coreVersion()`)
- [ ] Replace prototype extraction with Readability and per-site adapters
- [ ] Adblock network/cosmetic filtering with per-site exceptions
- [ ] Android TTS and selectable Vietnamese voices
- [ ] Cloud TTS provider with user-managed credentials
- [ ] Media3 MediaSessionService and background audio
- [ ] Next-chapter detection and playback queue
- [ ] SQLite library and progress persistence
- [ ] Offline TTS model evaluation
- [ ] CI tests, security review, signed release

## Milestones and gates

### M1 Foundation
Rust core compiles and tests pass; Android debug APK builds; JNI smoke test returns version 1.

### M2 Browser and Reader
Support tab/navigation lifecycle; safe URL validation; extraction fixtures across 30 sites; fallback manual selection.

### M3 TTS and playback
Android TTS voice enumeration, offline detection, Media3 audio session, foreground media service, resume state.

### M4 Cloud, offline, chapters
Opt-in cloud TTS; provider-specific quotas; local audio caching; chapter loop prevention and prefetch.

### M5 Adblock and library
Rule-engine license audit; request/cosmetic filtering; exceptions; SQLite persistence and migrations.

### M6 Release
Instrumented tests on Android 8, 12, 14 and 15; screen-off test; signed APK; changelog and security checklist.

## Architecture decisions

- Kotlin owns Android UI, WebView and Media3.
- Rust owns extraction, chapter logic, text normalization and eventual adblock rules.
- JNI/UniFFI interface must use versioned, explicit DTOs and bounded payload sizes.
- No claims of universal extraction or 100% ad blocking.
- No scraping of protected or access-restricted content.

## Known limitations of this initial scaffold

- Browser is a prototype: no tab manager, no adblock, no TTS, no Media3.
- Reader button reads the page body or article, not robust novel extraction.
- Rust core is only called for the version smoke test; domain APIs are not exposed yet.
- WebView navigation and lifecycle need hardening before release.
- The debug APK builds, but no device/emulator smoke test has been run yet.
