# Universal Reader

Open-source Android reader: Kotlin/Compose shell + portable Rust core.

**Status: M1 foundation.** The debug APK builds, the Rust core is unit-tested, and
Kotlin calls the native core through JNI (`NativeBridge.coreVersion()`). Reader
extraction, TTS, ad blocking and background playback are still on the roadmap.

## Quick start

Windows (PowerShell):

```powershell
. .\tools\env.ps1          # JDK / Android SDK / NDK / MinGW / cargo
.\tools\build-all.cmd      # native libs -> debug APK -> APK verification
```

Output: `android/app/build/outputs/apk/debug/app-debug.apk`.

The Gradle wrapper (8.13) is included. `android/local.properties` is machine-local
and must not be committed.

## Documentation

- [PLAN.md](PLAN.md) — scope, milestones, gates, risks.
- [BUILD.md](BUILD.md) — toolchain matrix, build commands, quality gates.
- [docs/ROADMAP.md](docs/ROADMAP.md) — original roadmap.

## Layout

```text
android/                 Kotlin/Compose app, WebView prototype, JNI bridge
rust/crates/reader-core/ Portable text, chapter and JNI code
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
.\gradlew.bat :app:assembleDebug
```

The native library is built separately by `tools/build-native.ps1` for
`arm64-v8a` and `x86_64`. See [BUILD.md](BUILD.md) for the full matrix.

## Security notes

The browser permits JavaScript, as required for many websites, but does not expose a
JavaScript-to-native bridge. No API keys are bundled. Reader extraction is local. Do
not treat this prototype as a hardened general-purpose browser.

## License

MIT — see [LICENSE](LICENSE).
