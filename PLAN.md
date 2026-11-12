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

### Đã hoàn thành

- [x] Giải nén scaffold vào `universal-reader/`.
- [x] `gradle.properties` (`android.useAndroidX=true`, JVM args, caching).
- [x] Gradle wrapper 8.13 + `local.properties` trỏ SDK.
- [x] `cargo test --workspace`: **4/4 test pass**.
- [x] `cargo fmt --check` và `cargo clippy -D warnings`: pass.
- [x] JNI bridge thật: `NativeBridge.coreVersion()` → `Java_com_universalreader_app_core_NativeBridge_coreVersion`.
- [x] Build `.so` cho `arm64-v8a` + `x86_64` bằng cargo-ndk; copy vào `jniLibs`.
- [x] APK debug build thành công, chứa cả hai ABI; symbol JNI và căn chỉnh 16 KB đã kiểm tra.
- [x] `:app:lintDebug`: pass (chỉ còn warning).
- [x] Script build chuẩn: `tools/env.ps1`, `build-native.ps1`, `build-all.ps1`, `verify-apk.ps1`.
- [x] Git repo `main`, push GitHub `wuwenhou/Universal-Reader`, tag `v0.1.0` + release APK.
- [x] CI xanh lần đầu: job `rust` (fmt/clippy/test) và `android` (native + APK + lint + verify).

### Chưa hoàn thành

- [ ] **M1 gate trên thiết bị thật**: cài APK, mở app, thấy `core v1`.
- [ ] Chưa có thiết bị adb; emulator chưa có hypervisor.
- [x] Đã tải AEHD 2.2.0 (`sdkmanager extras;google;Android_Emulator_Hypervisor_Driver`), **chưa cài** vì cần quyền admin.
- [ ] System image `system-images;android-35;google_apis;x86_64` chỉ còn thư mục `.installer` (chưa cài đủ) → chưa tạo được AVD.

## 3. Quyết định kiến trúc

| ID | Quyết định | Lý do | Xem lại khi |
|---|---|---|---|
| ADR-001 | Dùng **JNI thuần** cho M1, chưa dùng UniFFI | Ít dependency, đủ cho 1 hàm smoke test | Khi DTO > 3 hoặc cần schema versioned |
| ADR-002 | Windows build host dùng **GNU toolchain + MinGW** | Máy không có MSVC C++ linker; GNU đã cài sẵn | Khi cài Visual Studio Build Tools |
| ADR-003 | Chỉ build `arm64-v8a` + `x86_64` | Thiết bị thật + emulator; giảm APK size | Khi cần hỗ trợ armv7 |
| ADR-004 | `minSdk 26`, `compileSdk/targetSdk 35` | Theo scaffold; Android 8+ | Khi lên target 36 |
| ADR-005 | Extraction chạy **Readability.js trong WebView**, Rust xử lý hậu kỳ | DOM đã render, xử lý được site động; không mở JS→native bridge | Nếu cần trích xuất offline từ HTML thô |
| ADR-006 | Không dùng `addJavascriptInterface` | Giảm bề mặt tấn công XSS | Không dự kiến |

## 4. Lộ trình

### M1 — Foundation (đang ở đây)

- [x] Rust workspace compile + test.
- [x] Android debug APK build.
- [x] JNI symbol + native libs đóng gói.
- [ ] Smoke test trên thiết bị: hiển thị `core v1`.

**Gate:** APK cài được, mở app không crash, dòng tiêu đề hiện `core v1`.

### M2 — Browser + Reader

- [ ] Tách `BrowserViewModel`, `WebViewClient`, `WebChromeClient`.
- [ ] Address bar đồng bộ URL thật; progress; BackHandler; destroy WebView đúng lifecycle.
- [ ] `UrlPolicy`: chỉ https (hoặc whitelist http), chặn `intent://`, `file://`, `javascript:`, `data:`.
- [ ] Không bypass SSL error; bật Safe Browsing; tắt file access.
- [ ] Readability.js trong `assets/`, inject bằng `evaluateJavascript`, trả JSON.
- [ ] Fallback chọn thủ công khi extraction thất bại.
- [ ] `SiteAdapter` theo host + selector; fixture HTML cho 30 site.
- [ ] Rust: chuẩn hóa đoạn theo dòng trống, chunk TTS theo dấu câu Việt.
- [ ] Rust: canonicalize URL + chống lặp chương.

**Gate:** 30 fixture cho kết quả ổn định; back/forward/xoay màn hình không mất trang.

### M3 — TTS + playback

- [ ] Liệt kê voice `vi-VN`, phát hiện voice offline.
- [ ] Hàng đợi câu, tốc độ, cao độ, resume theo URL.
- [ ] Foreground service `mediaPlayback`, audio focus, notification.
- [ ] Quyết định: TTS trực tiếp + MediaSession hay `synthesizeToFile` + Media3.
- [ ] Test tắt màn hình, ngắt cuộc gọi, rút tai nghe.

**Gate:** nghe liên tục 30 phút khi tắt màn hình; resume đúng câu.

### M4 — Cloud + chương

- [ ] Provider cloud do user tự nhập key; lưu bằng Keystore/EncryptedSharedPreferences.
- [ ] Quota, retry/backoff, cache audio LRU.
- [ ] Phát hiện chương tiếp bằng adapter + heuristic `rel=next`.
- [ ] Prefetch N chương, giới hạn depth, chống lặp.
- [ ] Đánh giá TTS offline (Piper/Sherpa-onnx) nếu cần.

**Gate:** chuyển chương tự động 5 chương liên tiếp không lặp; key không lộ trong log.

### M5 — Adblock + thư viện

- [ ] Audit license `adblock-rust` (MPL-2.0) và filter list trước khi code.
- [ ] Network filter qua `shouldInterceptRequest`, cosmetic filter bằng CSS injection.
- [ ] Ngoại lệ theo site; cập nhật filter list có attribution.
- [ ] Room + migration test cho library/progress/settings.

**Gate:** trang chuẩn vẫn đọc được khi bật adblock; có nút tắt theo site.

### M6 — Release

- [ ] Instrumented test API 26/31/34/35.
- [ ] Test screen-off, background, process death.
- [ ] R8/minify, baseline profile, ký release.
- [ ] CI xanh, security checklist, changelog.
- [ ] Kiểm tra 16 KB page alignment trước khi phát hành.

**Gate:** APK release ký, cài sạch, chạy ổn định trên 4 phiên bản Android.

## 5. Phạm vi MVP đề xuất

Làm **M1 + M2 + M3 (TTS offline) + chuyển chương cơ bản** trước. Hoãn M4 cloud,
M5 adblock, M6 store cho giai đoạn sau. MVP này đã tạo ra app đọc truyện có ích.

## 6. Rủi ro

| Rủi ro | Ảnh hưởng | Giảm thiểu |
|---|---|---|
| Không có thiết bị test / emulator không tăng tốc | Không nghiệm thu được M1–M3 | Dùng máy thật qua adb; hoặc bật WHPX/AEHD |
| WebView chạy JS trên site lạ | XSS, lộ dữ liệu | Không JS bridge; URL policy; không lưu cookie nhạy cảm |
| License adblock | Không phát hành được | Audit trước; ưu tiên engine tự viết nếu cần |
| Cloud TTS key | Lộ key, chi phí | Key do user nhập; Keystore; quota; cache |
| Site đổi DOM | Extraction hỏng | Fixture + adapter + fallback thủ công |
| 16 KB page size | Play từ chối | Đã kiểm tra `0x4000`; giữ trong CI |

## 7. Definition of Done

Một task chỉ được coi là xong khi:

1. Code build được bằng `tools/build-all.cmd`.
2. Test liên quan pass; không nới lỏng test để build xanh.
3. `cargo fmt --check`, `cargo clippy -D warnings`, `lintDebug` không có lỗi mới.
4. Không thêm secret, key, keystore vào repo.
5. Hành vi mới có test hoặc bằng chứng chạy trên thiết bị.
6. Cập nhật PLAN.md/BUILD.md nếu thay đổi toolchain hoặc gate.
