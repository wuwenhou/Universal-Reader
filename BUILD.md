# Universal Reader - Build

Universal Reader is a React Native (Expo) app with native Android modules. It
is built from the [LNReader](https://github.com/LNReader/lnreader) codebase.

## Requirements

- Node >= 22.11.0
- pnpm 11.20.0 (`corepack enable pnpm`)
- JDK 17 or newer
- Android SDK with build tools 36 and NDK 28.2.13676358
- CMake 3.22.1 or newer (3.31.6 is recommended, see below)
- Rust 1.98 and `cargo-ndk` 4.1.2 (only needed for the native modules)

### CMake

The CMake version that ships with the Android SDK (3.22.1) re-runs the
configure step forever on Windows: the glob check it writes for
`CONFIGURE_DEPENDS` never settles and the build stops with

```
ninja: error: manifest 'build.ninja' still dirty after 100 tries
```

The project pins the version through the `cmakeVersion` property in
`android/gradle.properties`, which the config plugin writes into every
native module. Set it to a CMake version that is installed in the SDK:

```properties
cmakeVersion=3.31.6
```

Install it with the SDK manager:

```bash
sdkmanager "cmake;3.31.6"
```

### Room and SQLiteJDBC

The `native-background-tasks` module uses Room, whose annotation processor
needs the SQLiteJDBC native library. The processor unpacks it into a
temporary directory, which does not always work for the KAPT worker on
Windows. The config plugin therefore unpacks the library into the build
directory and points the processor at it.

The unpacked library is only needed for a local build; it is created by the
`unpackSqliteNative` task and is not part of the repository.

## Setup

```bash
pnpm install
pnpm run generate:env:debug
```

`generate:env:debug` writes `.env` and `src/generated/build-info.ts`. Both
are generated and must never be committed.

## Commands

```bash
pnpm run check          # format:check + lint + type-check
pnpm test               # jest (all projects)
pnpm run test:rn        # jest --selectProjects rn
pnpm run test:db        # jest --selectProjects db
pnpm run format         # prettier --write
```

Always run `pnpm run check` and `pnpm test` before considering a change
complete.

## Native modules

The native modules live in `modules/` and are written against
[Nitro](https://nitro.margelo.com). After changing a `*.nitro.ts` spec the
bindings have to be regenerated:

```bash
cd modules/nitro-tts
pnpm exec tsc --noEmit false
pnpm exec nitrogen
```

The generated bindings in `modules/nitro-tts/nitrogen` are committed; the
`lib` output is not.

## Building a release APK

```bash
pnpm run generate:env:release
pnpm exec expo prebuild --platform android --clean --no-install
cd android && ./gradlew :app:assembleRelease -PreleaseAbiSplits=true
```

The APKs are written to `android/app/build/outputs/apk/release`.

### Signing

The release build is signed with the keystore that the environment points
at. Nothing secret is ever committed:

| Variable                          | Meaning                       |
| --------------------------------- | ----------------------------- |
| `UNIVERSAL_READER_STORE_FILE`     | Absolute path to the keystore |
| `UNIVERSAL_READER_STORE_PASSWORD` | Keystore password             |
| `UNIVERSAL_READER_KEY_ALIAS`      | Key alias                     |
| `UNIVERSAL_READER_KEY_PASSWORD`   | Key password                  |

When the variables are missing the release build falls back to the debug
key, so a local build still works.

## Release

The `Release Android` workflow builds a draft release from `main`:

1. It resolves the next version and commits it.
2. It restores the keystore from the `UNIVERSAL_READER_KEYSTORE_BASE64`
   secret.
3. It builds the APKs, packages them and verifies their signatures.
4. It pushes the release commit and creates a draft GitHub release.

The repository secrets it needs:

| Secret                             | Meaning                      |
| ---------------------------------- | ---------------------------- |
| `UNIVERSAL_READER_KEYSTORE_BASE64` | The keystore, base64 encoded |
| `UNIVERSAL_READER_STORE_PASSWORD`  | Keystore password            |
| `UNIVERSAL_READER_KEY_ALIAS`       | Key alias                    |
| `UNIVERSAL_READER_KEY_PASSWORD`    | Key password                 |

Optional repository variables: `MYANIMELIST_CLIENT_ID` and
`ANILIST_CLIENT_ID`.

## Filter lists

The uBO filter lists in `assets/adblock` are updated by hand. To refresh
them, download the new files and keep the names:

```bash
curl -o assets/adblock/easylist.txt https://easylist.to/easylist/easylist.txt
curl -o assets/adblock/easyprivacy.txt https://easylist.to/easylist/easyprivacy.txt
curl -o assets/adblock/abpvn.txt https://raw.githubusercontent.com/abpvn/abpvn/master/filter/abpvn_ublock.txt
```

They are inlined into the bundle at build time, so the app blocks ads
without downloading anything.
