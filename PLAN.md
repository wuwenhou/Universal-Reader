# Universal Reader — Kế hoạch triển khai

Tài liệu này là nguồn sự thật cho phạm vi, thứ tự công việc và tiêu chí nghiệm thu.
Chuẩn build/toolchain nằm trong [BUILD.md](BUILD.md).

## 1. Mục tiêu

App Android đọc nội dung web/truyện chữ, có:

- Browser an toàn tối thiểu (WebView, URL policy, back navigation).
- Trích nội dung bài/chương bằng Readability + adapter theo site.
- TTS tiếng Việt (offline trước, cloud sau), nghe nền, chương tiếp.
- Thư viện + tiến độ đọc lưu cục bộ.
- Adblock có ngoại lệ theo site (sau cùng, phụ thuộc license).

Không hứa hẹn "trích xuất mọi site" hay "chặn 100% quảng cáo". Không scrape nội dung
trả phí hoặc yêu cầu đăng nhập.

## 2. Trạng thái hiện tại — 2026-11-12

Phiên bản phát hành: **0.2.0** (`versionCode 2`).

### Đã hoàn thành

- [x] Rust core v2: 9/9 unit test pass; `fmt --check` và `clippy -D warnings` sạch.
- [x] JNI: `coreVersion`, `normalizeParagraphs`, `ttsChunks`, `canonicalizeUrl`.
- [x] Native libs `arm64-v8a` + `x86_64`, kiểm tra đủ 4 symbol và căn chỉnh 16 KB.
- [x] `UrlPolicy`: chỉ HTTPS; chặn `http`, `javascript:`, `file:`, `content:`, `data:`, `intent:`.
- [x] WebView hardening: không `addJavascriptInterface`, tắt file/content access,
      chặn mixed content, không bỏ qua SSL error, bật Safe Browsing.
- [x] Readability.js trong `assets/` + license Apache-2.0; adapter cho 45 host.
- [x] Fallback bôi đen đoạn văn khi extraction thất bại.
- [x] Reader UI: tiêu đề, số đoạn/từ, cỡ chữ, chương tiếp theo, chống lặp chương.
- [x] TTS: hàng đợi câu, tốc độ/cao độ, resume theo chunk, foreground service
      `mediaPlayback`, audio focus, MediaSession, notification, wake lock.
- [x] Tự chuyển chương + tự phát TTS chương kế khi đọc xong (có thể tắt).
- [x] Thư viện + tiến độ: SharedPreferences + JSON codec thuần Kotlin.
- [x] Adblock: host/path list tự viết + CSS cosmetic hẹp + ngoại lệ theo site.
- [x] 27 JVM unit test cho URL policy, adblock, library codec, playback queue,
      chapter guard và site adapter registry.
- [x] APK debug + APK release R8 (minify/shrink) build thành công.
- [x] CI xanh: Rust fmt/clippy/test + Android unit test/debug/release/lint/APK verify.
- [x] Icon adaptive, backup rules, release signing đọc từ biến môi trường.

### Chưa hoàn thành / cần môi trường

- [ ] **Device smoke test**: không có adb device; emulator không có hypervisor.
      Cần kiểm tra cài đặt, reader extraction thực tế, TTS tắt màn hình 30 phút,
      audio focus khi có cuộc gọi, rút tai nghe, xoay màn hình và process death.
- [ ] **Instrumented test** API 26/31/34/35 (chưa có thiết bị/emulator).
- [ ] **Cloud TTS** do user tự nhập key + Keystore/EncryptedSharedPreferences,
      quota/retry/cache audio. Hiện tại chỉ dùng voice cài sẵn trên thiết bị.
- [ ] **Room/SQLite** cho library/progress/settings. MVP dùng SharedPreferences + JSON.
- [ ] Prefetch nhiều chương và giới hạn depth (mới có chuyển từng chương + chống lặp).
- [ ] Adblock filter list cập nhật/attribution. Hiện tại dùng danh sách tự viết.
- [ ] Đánh giá TTS offline Piper/Sherpa-onnx.
- [ ] Release signing key đã tạo ngoài repo; cần backup an toàn trước khi phát hành
      production (xem BUILD.md mục 7).

## 3. Quyết định kiến trúc

| ID | Quyết định | Lý do | Xem lại khi |
|---|---|---|---|
| ADR-001 | Dùng **JNI thuần** cho core | Ít dependency, marshalling rõ ràng, test Rust trên host | Khi DTO phức tạp hơn hoặc cần schema versioned |
| ADR-002 | Windows build host dùng **GNU toolchain + MinGW** | Máy không có MSVC C++ linker | Khi cài Visual Studio Build Tools |
| ADR-003 | Chỉ build `arm64-v8a` + `x86_64` | Thiết bị thật + emulator; giảm APK size | Khi cần hỗ trợ armv7 |
| ADR-004 | `minSdk 26`, `compileSdk/targetSdk 35` | Android 8+; AGP 8.7.3 hỗ trợ compileSdk 35 | Khi lên AGP/compileSdk 36 |
| ADR-005 | Extraction chạy **Readability.js trong WebView**, Rust xử lý hậu kỳ | DOM đã render, xử lý được site động; không mở JS→native bridge | Nếu cần trích xuất offline từ HTML thô |
| ADR-006 | Không dùng `addJavascriptInterface` | Giảm bề mặt tấn công XSS | Không dự kiến |
| ADR-007 | TTS trực tiếp + platform `MediaSession` + foreground service, không Media3 | Ít dependency, đủ notification/audio focus, tránh `synthesizeToFile` | Khi cần gapless/audio cache chuyên nghiệp |
| ADR-008 | Library dùng **SharedPreferences + JSON** | Dataset nhỏ, codec thuần Kotlin dễ test | Khi cần query/migration → Room |
| ADR-009 | Adblock tự viết host/path + cosmetic CSS | Tránh license MPL/filter-list và phụ thuộc mạng | Khi cần tỉ lệ chặn cao hơn |
| ADR-010 | 45 adapter chỉ là **hint**, luôn fallback Readability | Site đổi DOM thường xuyên | Khi có fixture/integration test |

## 4. Lộ trình

### M1 — Foundation

- [x] Rust workspace compile + test.
- [x] Android debug APK build.
- [x] JNI symbol + native libs đóng gói.
- [ ] Smoke test trên thiết bị: hiển thị `core v2`.

**Gate:** APK cài được, mở app không crash, Settings hiện `Native core: v2`.

### M2 — Browser + Reader

- [x] Tách `BrowserViewModel`, `WebViewClient`, `WebChromeClient`.
- [x] Address bar đồng bộ URL thật; progress; BackHandler; WebView lifecycle.
- [x] `UrlPolicy`: chỉ HTTPS, chặn scheme nguy hiểm.
- [x] Không bypass SSL error; Safe Browsing; tắt file access; chặn mixed content.
- [x] Readability.js trong `assets/`, inject bằng `evaluateJavascript`, trả JSON.
- [x] Fallback chọn thủ công khi extraction thất bại.
- [x] `SiteAdapter` theo host + selector cho 45 host.
- [x] Rust: chuẩn hóa đoạn, chunk TTS theo dấu câu Việt/CJK.
- [x] Rust: canonicalize URL + chống lặp chương.

**Gate:** unit test adapter registry pass; cần device test back/forward/xoay màn hình.

### M3 — TTS + playback

- [x] Liệt kê voice `vi-VN`, phát hiện voice offline.
- [x] Hàng đợi câu, tốc độ, cao độ, resume theo URL.
- [x] Foreground service `mediaPlayback`, audio focus, notification, MediaSession.
- [x] Quyết định: TTS trực tiếp + MediaSession (ADR-007).
- [ ] Test tắt màn hình, ngắt cuộc gọi, rút tai nghe (cần thiết bị).

**Gate:** nghe liên tục 30 phút khi tắt màn hình; resume đúng câu.

### M4 — Cloud + chương

- [ ] Provider cloud do user tự nhập key; lưu bằng Keystore/EncryptedSharedPreferences.
- [ ] Quota, retry/backoff, cache audio LRU.
- [x] Phát hiện chương tiếp bằng adapter + heuristic `rel=next`/nhãn tiếng Việt.
- [x] Chuyển chương tự động + tự phát TTS, chống lặp bằng canonical URL.
- [ ] Prefetch N chương, giới hạn depth.
- [ ] Đánh giá TTS offline (Piper/Sherpa-onnx) nếu cần.

**Gate:** chuyển chương tự động 5 chương liên tiếp không lặp; key không lộ trong log.

### M5 — Adblock + thư viện

- [x] Audit license: chọn engine tự viết, không dùng `adblock-rust`/filter list bên thứ ba.
- [x] Network filter qua `shouldInterceptRequest`, cosmetic filter bằng CSS injection.
- [x] Ngoại lệ theo site; bật/tắt toàn cục.
- [x] Library/progress/settings lưu cục bộ (SharedPreferences + JSON, ADR-008).
- [ ] Room + migration test (khi cần query/migration).

**Gate:** trang chuẩn vẫn đọc được khi bật adblock; có nút tắt theo site.

### M6 — Release

- [ ] Instrumented test API 26/31/34/35 (cần thiết bị/emulator).
- [ ] Test screen-off, background, process death (cần thiết bị).
- [x] R8/minify, shrink resources, keep rule cho JNI, ký release.
- [x] CI xanh, security checklist, changelog, unit test.
- [x] Kiểm tra 16 KB page alignment trước khi phát hành.

**Gate:** APK release ký, cài sạch, chạy ổn định trên 4 phiên bản Android.

## 5. Phạm vi MVP

MVP hiện tại gồm M1 + M2 + M3 (TTS thiết bị) + chuyển chương + thư viện + adblock cơ bản.
Hoãn M4 cloud, prefetch nhiều chương, Room và filter list cập nhật sang giai đoạn sau.

## 6. Rủi ro

| Rủi ro | Ảnh hưởng | Giảm thiểu |
|---|---|---|
| Không có thiết bị test / emulator không tăng tốc | Không nghiệm thu được M1–M3, M6 | Dùng máy thật qua adb; hoặc bật WHPX/AEHD |
| WebView chạy JS trên site lạ | XSS, lộ dữ liệu | Không JS bridge; URL policy; không lưu cookie nhạy cảm |
| Site đổi DOM | Extraction hỏng | 45 adapter hint + Readability fallback + bôi đen thủ công |
| TTS thiếu voice vi-VN | Không đọc được | Thông báo rõ trong UI; hướng dẫn cài voice trong Settings Android |
| Audio focus/rút tai nghe | TTS chồng tiếng hoặc tiếp tục phát | Đã xử lý focus + `ACTION_AUDIO_BECOMING_NOISY`; cần test thiết bị |
| Cloud TTS key (tương lai) | Lộ key, chi phí | Key do user nhập; Keystore; quota; cache |
| Adblock quá tay | Hỏng layout/nội dung | CSS hẹp, host list nhỏ, ngoại lệ theo site |
| 16 KB page size | Play từ chối | Đã kiểm tra `0x4000`; giữ trong CI |
| Mất release keystore | Không thể cập nhật app | Keystore + mật khẩu lưu ngoài repo, backup an toàn |

## 7. Definition of Done

Một task chỉ được coi là xong khi:

1. Code build được bằng `tools/build-all.cmd`.
2. Test liên quan pass; không nới lỏng test để build xanh.
3. `cargo fmt --check`, `cargo clippy -D warnings`, `testDebugUnitTest`, `lintDebug`
   không có lỗi mới.
4. Không thêm secret, key, keystore vào repo.
5. Hành vi mới có test hoặc bằng chứng chạy trên thiết bị.
6. Cập nhật PLAN.md/BUILD.md nếu thay đổi toolchain hoặc gate.
