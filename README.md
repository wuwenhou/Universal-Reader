# Universal Reader

Open-source Android reader: Kotlin/Compose shell + portable Rust core.

**Status: 0.2.0 preview (M2–M6).** Reader mode extracts articles and chapters
with Mozilla Readability plus per-site hints, Vietnamese TTS plays in the
background through a foreground service and platform MediaSession, the local
library remembers progress, and a conservative ad filter can be disabled per
site. The debug and R8-minified release APKs build from a clean checkout.

This is not a hardened general-purpose browser and it does not promise
universal extraction or 100% ad blocking. It never bypasses paywalls, logins or
certificate errors.

**[Download the latest APK](https://github.com/wuwenhou/Universal-Reader/releases/latest)** —
0.2.0 is a preview build; read the release notes before installing.

## Features

- **Safe browser shell** — HTTPS-only URL policy, no JavaScript-to-native
  bridge, file/content access disabled, mixed content blocked, SSL errors
  always cancelled, Safe Browsing enabled.
- **Reader mode** — Readability.js runs inside the WebView; 45 curated site
  adapters act as hints and fall back to Readability when a site changes.
  Manual text selection is the last fallback.
- **Vietnamese TTS** — sentence queue, speed/pitch control, resume by chunk,
  audio focus handling, headphone-unplug pause, wake lock, notification and
  platform MediaSession controls. Optional automatic next-chapter playback.
- **Library** — locally stored books, last chapter and TTS progress, with a
  pure-Kotlin JSON codec ready for a future Room migration.
- **Ad filter** — a small self-authored host/path list and narrow cosmetic CSS,
  with a per-site exception toggle.
- **Portable Rust core** — paragraph normalization, Unicode-safe TTS chunking,
  URL canonicalization and chapter-loop prevention through JNI.

## Limitations

- No test device was available for this build. Screen-off playback, audio-focus
  interruptions, rotation and process-death recovery still need a hardware
  smoke test.
- Cloud TTS with user-managed credentials is not implemented; playback uses the
  device's installed Vietnamese voice (offline or network-backed).
- The library uses SharedPreferences + JSON for the MVP, not Room/SQLite.
- Ad filtering is deliberately conservative and is not a full filter-list
  engine.
- Protected, login-only or paywalled content is out of scope.

## Quick start

Windows (PowerShell):

```powershell
. .\tools\env.ps1          # JDK / Android SDK / NDK / MinGW / cargo
.\tools\build-all.cmd      # native libs -> debug APK -> APK verification
```

Output: `android/app/build/outputs/apk/debug/app-debug.apk`.

The Gradle wrapper (8.13) is included. `android/local.properties` is
machine-local and must not be committed.

## Documentation

- [PLAN.md](PLAN.md) — scope, milestones, gates, risks.
- [BUILD.md](BUILD.md) — toolchain matrix, build commands, quality gates.
- [CHANGELOG.md](CHANGELOG.md) — release history.
- [SECURITY.md](SECURITY.md) — security model and reporting.
- [docs/ROADMAP.md](docs/ROADMAP.md) — original roadmap and status.

## Layout

```text
android/                 Kotlin/Compose app, WebView reader, TTS, library
rust/crates/reader-core/ Portable text, URL, chapter and JNI code
tools/                   env, native build, all-in-one build, APK verify
```

## Rust

```powershell
cd rust
cargo +stable-x86_64-pc-windows-gnu test --workspace
```

## Android

```powershell
cd android
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease   # unsigned unless UR_KEYSTORE is set
```

The native library is built separately by `tools/build-native.ps1` for
`arm64-v8a` and `x86_64`. See [BUILD.md](BUILD.md) for the full matrix.

## Security notes

The browser permits JavaScript, as required by many websites, but does not
expose a JavaScript-to-native bridge. No API keys are bundled. Reader
extraction and TTS run locally. The app does not log page content or credentials.
Do not treat this preview as a hardened general-purpose browser.

## License

MIT — see [LICENSE](LICENSE). Readability.js is Apache-2.0; its license is
bundled at `android/app/src/main/assets/readability.LICENSE`.
