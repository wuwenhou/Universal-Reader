# Security Policy

## Supported versions

| Version | Supported |
|---|---|
| 0.2.x | Yes (preview) |
| 0.1.x | No |

Universal Reader is pre-1.0 software. Security fixes are applied to the latest
release on `main`.

## Reporting a vulnerability

Please do **not** open a public issue for a vulnerability.

Use GitHub's private vulnerability reporting:
<https://github.com/wuwenhou/Universal-Reader/security/advisories/new>

Include the affected version, reproduction steps, impact and any suggested fix.
You will receive an acknowledgement as soon as possible. Please allow time for a
fix before public disclosure.

## Security model

- **Network:** only `https://` URLs are loaded. `http`, `javascript:`, `file:`,
  `content:`, `data:`, `blob:` and `intent:` URLs are rejected before `loadUrl`.
- **WebView:** JavaScript is enabled because most sites and Readability require
  it. There is **no** `addJavascriptInterface` bridge, so page JavaScript cannot
  call Kotlin or Rust directly. File/content access, mixed content and
  JavaScript-opened windows are disabled. Safe Browsing is enabled.
- **TLS:** SSL certificate errors are always cancelled; the app never installs a
  custom `SslErrorHandler` bypass.
- **Extraction:** Readability.js and site adapters run inside the rendered page.
  Extracted text is passed back as JSON through `evaluateJavascript`.
- **Storage:** library, progress and settings are stored in app-private
  SharedPreferences. Backup and device transfer are disabled.
- **TTS:** playback uses the device's installed Text-to-Speech engine. The app
  does not upload page text to a cloud provider.
- **Ad filter:** a self-authored host/path list and narrow cosmetic CSS. It is a
  privacy aid, not a guarantee that all ads/trackers are blocked.
- **Credentials:** no API keys, tokens or signing keys are bundled. Release
  signing reads keystore paths/passwords from environment variables and the
  keystore lives outside the repository.

## Out of scope

- Bypassing paywalls, logins, DRM or access controls.
- Guaranteeing extraction on every website.
- Blocking 100% of advertisements or trackers.
- Vulnerabilities in third-party websites loaded by the user.
- Devices with a compromised OS, rooted modifications or malicious TTS engines.

## Dependency licenses

- Mozilla Readability.js — Apache-2.0. License bundled at
  `android/app/src/main/assets/readability.LICENSE`.
- Rust crates and AndroidX libraries — permissive licenses (MIT/Apache-2.0) as
  declared by their upstream projects.
- The ad filter list and cosmetic rules are original to this project.

## Release integrity

Release APKs are built from the tagged commit; CI verifies that the same commit
passes the Rust gates, Android unit tests and lint, and that the packaged native
libraries expose the expected JNI symbols with 16 KB page alignment. Verify the
APK signature and compare the SHA-256 checksum published with the release. Do
not install builds from untrusted mirrors.
