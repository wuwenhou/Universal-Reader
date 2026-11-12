# Universal Reader — Chuẩn BUILD

Mọi build phải đi qua các bước trong tài liệu này. Nếu thay đổi toolchain, cập nhật
bảng phiên bản và ghi lý do vào PR.

## 1. Toolchain matrix

| Thành phần | Phiên bản / đường dẫn trên máy này | Ghi chú |
|---|---|---|
| OS | Windows 10.0.26100 (x64) | |
| JDK | Temurin **21.0.12.1** — `E:\VScode_Opencode\Tools\jdk` | AGP 8.7 cần JDK 17+ |
| Gradle | **8.13** qua wrapper | Không dùng Gradle 9 với AGP 8.7 |
| AGP | 8.7.3 | |
| Kotlin | 2.0.21 + Compose compiler plugin | |
| Compose | ui/foundation 1.7.5, material3 1.3.1 | |
| Lifecycle | viewmodel/runtime-compose 2.8.7 | `collectAsStateWithLifecycle`, `viewModelScope` |
| Coroutines | kotlinx-coroutines-android 1.9.0 | TTS/library state |
| compileSdk / targetSdk | 35 | AGP 8.7.3 hỗ trợ tối đa compileSdk 35 |
| minSdk | 26 | Android 8.0+ |
| App version | `versionCode 2`, `versionName 0.2.0` | SemVer, tăng mỗi release |
| Android SDK | `C:\Users\OWG\android-sdk` | platforms 34/35/36, build-tools 35/36 |
| NDK | **28.2.13676358** | Khai báo `ndkVersion` trong `app/build.gradle.kts` |
| Rust (mặc định) | stable-x86_64-pc-windows-**msvc** 1.98.1 | Không dùng để build do thiếu MSVC linker |
| Rust (host build) | stable-x86_64-pc-windows-**gnu** 1.99.1 | Dùng cho `cargo test` và cargo-ndk |
| Rust Android targets | `aarch64-linux-android`, `x86_64-linux-android` | Cài cho toolchain GNU |
| MinGW-w64 | WinLibs **16.2.0** UCRT POSIX SEH — `E:\VScode_Opencode\Tools\mingw64\mingw64` | Cung cấp linker cho toolchain GNU |
| cargo-ndk | **4.1.2** | `cargo +stable-x86_64-pc-windows-gnu install cargo-ndk --locked` |
| adb | SDK platform-tools | Tránh trộn với bản adb khác trên PATH |

### Vì sao dùng toolchain GNU trên Windows

Máy này có Visual Studio 2019 nhưng **không cài workload C++**, nên `link.exe` không
tồn tại. Rust MSVC không link được. Giải pháp đã chọn:

- Host tests và cargo-ndk dùng `stable-x86_64-pc-windows-gnu`.
- Linker lấy từ MinGW-w64 (WinLibs).
- Build Android không phụ thuộc linker host; cargo-ndk dùng clang của NDK.

Nếu sau này cài "Visual Studio Build Tools → Desktop development with C++", có thể
quay lại toolchain MSVC bằng cách bỏ tham số `+stable-...-gnu`.

## 2. Cài đặt một lần

Các bước dưới đây đã hoàn tất trên máy hiện tại; ghi lại để tái lập.

```powershell
# 1. JDK 21
#    Đặt tại E:\VScode_Opencode\Tools\jdk

# 2. Android SDK + NDK
#    C:\Users\OWG\android-sdk
sdkmanager "platforms;android-35" "build-tools;35.0.0" "ndk;28.2.13676358"

# 3. MinGW-w64 (WinLibs)
#    Giải nén vào E:\VScode_Opencode\Tools\mingw64\mingw64

# 4. Rust targets cho toolchain GNU
rustup target add --toolchain stable-x86_64-pc-windows-gnu `
  aarch64-linux-android x86_64-linux-android

# 5. cargo-ndk
cargo +stable-x86_64-pc-windows-gnu install cargo-ndk --locked

# 6. Gradle wrapper (đã sinh sẵn trong repo)
#    android/gradlew.bat --version  ->  Gradle 8.13
```

`android/local.properties` chứa `sdk.dir` và **không được commit**.

## 3. Build

### 3.1 Thiết lập môi trường

```powershell
# Cách A: dùng script (khuyến nghị)
. .\tools\env.ps1

# Cách B: tự set
$env:JAVA_HOME = 'E:\VScode_Opencode\Tools\jdk'
$env:ANDROID_HOME = 'C:\Users\OWG\android-sdk'
$env:ANDROID_NDK_HOME = 'C:\Users\OWG\android-sdk\ndk\28.2.13676358'
$env:Path = 'E:\VScode_Opencode\Tools\mingw64\mingw64\bin;' + $env:Path
```

Nếu PowerShell chặn script:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\build-all.ps1
```

### 3.2 Build native libraries

```powershell
.\tools\build-native.ps1            # release, arm64-v8a + x86_64
.\tools\build-native.ps1 -Profile debug
```

Script thực hiện:

```text
cargo +stable-x86_64-pc-windows-gnu ndk -P 26 `
  -t arm64-v8a -t x86_64 `
  -o ..\android\app\src\main\jniLibs build --release
```

Sau đó kiểm tra:

- Tồn tại `libreader_core.so` cho từng ABI.
- Symbol `Java_com_universalreader_app_core_NativeBridge_coreVersion` có trong bảng export.
- LOAD segment căn chỉnh `0x4000` (16 KB).

`jniLibs/` là output, không commit.

### 3.3 Build APK và chạy unit test

```powershell
cd android
.\gradlew.bat :app:testDebugUnitTest --console=plain
.\gradlew.bat :app:assembleDebug --console=plain
```

APK: `android/app/build/outputs/apk/debug/app-debug.apk`.
Kết quả test: `android/app/build/test-results/testDebugUnitTest/`.

Build release (R8 + shrink resources; không ký nếu thiếu `UR_KEYSTORE`):

```powershell
.\gradlew.bat :app:assembleRelease --console=plain
```

APK: `android/app/build/outputs/apk/release/app-release.apk` (có ký) hoặc
`app-release-unsigned.apk` (không có keystore).

### 3.4 Build tất cả + verify

```powershell
.\tools\build-all.cmd
```

Tương đương: native → `testDebugUnitTest` → `assembleDebug` → `verify-apk.ps1`.
Script kiểm tra cả 4 symbol JNI và căn chỉnh `0x4000`.

### 3.5 Cài lên thiết bị

```powershell
$adb = "$env:ANDROID_HOME\platform-tools\adb.exe"
& $adb devices -l
& $adb install -r .\android\app\build\outputs\apk\debug\app-debug.apk
& $adb shell am start -n com.universalreader.app/.MainActivity
```

Nghiệm thu M1/M2: mở Settings, dòng native hiển thị `Native core: v2 · JNI OK`.
Sau đó thử: mở một bài viết, bấm **Đọc**, kiểm tra reader; bấm **Đọc** trong
reader để nghe TTS; tắt màn hình và kiểm tra notification/media controls.

## 4. Quality gates

Chạy trước mỗi PR/merge. Mục tiêu: **không có lỗi mới**, warning phải được ghi nhận.

| Gate | Lệnh | Bắt buộc |
|---|---|---|
| Rust format | `cargo +stable-x86_64-pc-windows-gnu fmt --all -- --check` | Có |
| Rust lint | `cargo +stable-x86_64-pc-windows-gnu clippy --workspace --all-targets -- -D warnings` | Có |
| Rust test | `cargo +stable-x86_64-pc-windows-gnu test --workspace` | Có |
| Android unit test | `.\gradlew.bat :app:testDebugUnitTest` | Có |
| Android lint | `.\gradlew.bat :app:lintDebug` | Có |
| APK debug | `.\gradlew.bat :app:assembleDebug` | Có |
| APK release (R8) | `.\gradlew.bat :app:assembleRelease` | Có |
| APK verify | `.\tools\verify-apk.ps1` | Có |
| Device smoke | cài + mở app + `adb logcat` không crash | Có (M1+) |
| TTS device test | screen-off 30 phút, audio focus, rút tai nghe | Có (M3+) |
| Instrumented test | `.\gradlew.bat :app:connectedDebugAndroidTest` | Khi có test |

Chạy nhanh toàn bộ gate Rust:

```powershell
cd rust
cargo +stable-x86_64-pc-windows-gnu fmt --all -- --check
cargo +stable-x86_64-pc-windows-gnu clippy --workspace --all-targets -- -D warnings
cargo +stable-x86_64-pc-windows-gnu test --workspace
```

## 5. Artifact và versioning

- Debug APK: `app-debug.apk` — chỉ dùng nội bộ.
- Release: `universal-reader-<versionName>-<abi>.apk` hoặc `.aab` cho store.
- `versionCode`: tăng đơn điệu mỗi lần phát hành.
- `versionName`: SemVer (`0.1.0`, `0.2.0`, ...).
- Không đổi `applicationId` (`com.universalreader.app`) sau khi phát hành.
- APK/AAB không commit; chỉ commit source + lockfile.

## 6. Native library policy

- ABI hỗ trợ: `arm64-v8a` (thiết bị thật), `x86_64` (emulator).
- Không thêm ABI mới nếu chưa có test.
- API level tối thiểu: 26 (`-P 26`).
- Release phải strip symbol; debug có thể giữ.
- Kiểm tra 16 KB alignment (`0x4000`) trong CI trước khi phát hành.
- Mọi thay đổi JNI phải bump `CORE_VERSION` và cập nhật test.

## 7. Signing và release

- Keystore đặt **ngoài repo**; không commit `*.jks`/`*.keystore`.
- Gradle release đọc biến môi trường:
  `UR_KEYSTORE`, `UR_KEYSTORE_PASSWORD`, `UR_KEY_ALIAS`, `UR_KEY_PASSWORD`.
- Không log mật khẩu/khóa. Nếu biến môi trường từng bị in ra log, hãy tạo key mới.
- Tạo keystore mới (một lần; lưu file + mật khẩu ở nơi an toàn):

```powershell
keytool -genkeypair -v -keystore "$env:USERPROFILE\keys\universal-reader-release.jks" `
  -alias universal-reader -keyalg RSA -keysize 4096 -validity 10000 `
  -storetype PKCS12
```

- Build có ký:

```powershell
$env:UR_KEYSTORE = "$env:USERPROFILE\keys\universal-reader-release.jks"
$env:UR_KEYSTORE_PASSWORD = '<store-password>'
$env:UR_KEY_ALIAS = 'universal-reader'
$env:UR_KEY_PASSWORD = '<key-password>'
cd android
.\gradlew.bat :app:assembleRelease --console=plain
```

- Trước release: chạy `verify-apk.ps1 -Apk <release.apk>`, kiểm tra
  `apksigner verify --verbose`, và lưu mapping R8 để đọc crash stack trace.
- R8/minify + shrink resources đã bật; keep rule cho JNI nằm trong
  `android/app/proguard-rules.pro`.
- APK debug dùng debug key; không phát hành production bằng debug key.
- APK release dùng APK Signature Scheme v2 (API 24+) và v3 (API 28+); v1 bị tắt
  vì `minSdk 26`. `apksigner verify` phải báo v2 và v3 `true`.
- Nếu đổi signing key, người dùng cũ phải gỡ app trước khi cài bản mới.

## 8. CI

Workflow mẫu: `.github/workflows/ci.yml`.

- Job `rust`: fmt, clippy, test trên Ubuntu.
- Job `android`: JDK 21, Android SDK 35, NDK 28.2, Rust targets, cargo-ndk,
  build native, `testDebugUnitTest`, `assembleDebug`, `assembleRelease`,
  `lintDebug`, verify 4 JNI symbol + 16 KB alignment.
- Dùng `android-actions/setup-android@v4`: v3 cài package `tools` mặc định,
  nhưng package này đã bị xóa khỏi SDK repository (lỗi `Failed to find package 'tools'`).
- `android/gradlew` phải có mode `100755` trong git; nếu không runner báo
  `Permission denied` (exit 126). Kiểm tra: `git ls-files -s android/gradlew`.
- CI không build release nếu thiếu secret ký.
- Cache: Gradle và Cargo để giữ thời gian build dưới ~10 phút.

## 9. Troubleshooting

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `link.exe not found` | Thiếu MSVC C++ workload | Dùng toolchain GNU + MinGW, hoặc cài VS Build Tools C++ |
| `unknown package: 26` | cargo-ndk dùng `-p` (package) thay vì `-P` (platform) | Đổi thành `-P 26` |
| `This project uses AndroidX...` | Thiếu `android.useAndroidX=true` | Đã có trong `android/gradle.properties` |
| Gradle báo không hỗ trợ AGP | Dùng Gradle 9 | Dùng wrapper 8.13 |
| `Unable to strip libreader_core.so` | Thiếu `ndkVersion` | Đã set `ndkVersion = "28.2.13676358"` |
| `JAVA_HOME is not set` | Chưa source `env.ps1` | `. .\tools\env.ps1` |
| Script `.ps1` bị chặn | Execution policy | Dùng `.cmd` wrapper hoặc `-ExecutionPolicy Bypass` |
| CI: `./gradlew: Permission denied` (126) | `gradlew` mất exec bit khi commit từ Windows | `git update-index --chmod=+x android/gradlew` rồi commit |
| CI: `Failed to find package 'tools'` | `setup-android@v3` cài package `tools` đã bị xóa | Dùng `setup-android@v4` |
| Emulator không boot / quá chậm | Không có hypervisor | Dùng máy thật; hoặc bật WHPX/AEHD (admin + reboot) |
| `core v-1` trên app | `.so` thiếu hoặc sai ABI | Chạy `build-native.ps1`, kiểm tra `jniLibs` |
| TTS báo thiếu giọng tiếng Việt | Máy chưa cài voice `vi-VN` | Cài trong Settings → Language & input → Text-to-speech |
| TTS dừng sau một câu | Thiếu `UtteranceProgressListener` (đã sửa ở 0.2.0) | Kiểm tra logcat tag `TextToSpeech` |
| Release crash `UnsatisfiedLinkError` | R8 đổi tên class/method JNI | Giữ rule trong `proguard-rules.pro` |
| APK release `unsigned` | Chưa set `UR_KEYSTORE` | Set 4 biến môi trường rồi build lại |

## 10. Checklist trước merge

- [ ] `tools/build-all.cmd` chạy thành công.
- [ ] 4 gate Rust ở mục 4 pass.
- [ ] `testDebugUnitTest` pass (27 test ở 0.2.0).
- [ ] `lintDebug` không có error mới.
- [ ] `assembleRelease` pass (R8/minify).
- [ ] `verify-apk.ps1` pass (4 symbol + 16 KB).
- [ ] Không có secret/key/keystore trong diff.
- [ ] Cập nhật PLAN.md nếu thay đổi phạm vi/gate.
- [ ] Cập nhật BUILD.md nếu thay đổi toolchain/command.
