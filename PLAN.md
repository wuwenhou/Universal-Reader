# Universal Reader - Plan

Universal Reader is a fork of [LNReader](https://github.com/LNReader/lnreader)
that adds real ad blocking, a full screen listening player and a reader that
survives broken chapters.

## Status

| Area                                                                           | State |
| ------------------------------------------------------------------------------ | ----- |
| Fork of LNReader on `main`                                                     | done  |
| Rebrand (app id, scheme, version 0.3.0)                                        | done  |
| Ad blocking (uBO lists, HTML/cosmetic/network layers)                          | done  |
| Ad blocking settings (lists, per site exceptions)                              | done  |
| HTTPS enforcement with per source exception                                    | done  |
| Full screen listening player (sentence queue, seek, speed, voice, sleep timer) | done  |
| Mini player in the reader and the bottom navigation                            | done  |
| Native sleep timer in `TtsPlaybackStore`                                       | done  |
| Reader hardening (timeout, size cap, error boundary, WebView crash)            | done  |
| Release workflow with environment based signing                                | done  |

## Architecture

### Ad blocking

`src/services/adblock` holds the whole feature.

- `lists.ts` inlines the filter lists at build time. They are uBO syntax, so
  the bundled [Ghostery adblocker](https://github.com/ghostery/adblocker)
  engine can use them unchanged.
- `engine.ts` owns the `FiltersEngine`. It is built in the background after
  the app started, because parsing the lists takes a moment.
- `adblockService.ts` is the facade the rest of the app uses. It decides
  what may be blocked:
  - the app's own hosts (GitHub, Drive, the trackers) are never blocked,
  - a source site is never blocked - the chapter text and its images are
    needed to read,
  - a site the user excluded is never blocked.
- `htmlFilter.ts` removes blocked resources from a chapter and returns the
  cosmetic rules for the page.
- `fetchInterceptor.ts` wraps the global `fetch` so no request that matches
  a filter list leaves the device.
- `fetchInterceptor.ts` also rejects plain HTTP, unless the source was
  explicitly allowed in the settings.

The reader applies the result in three places: the chapter markup is
filtered before it is rendered, the cosmetic rules are injected into the
page, and the links the reader opens are checked.

What the blocker does **not** do: it cannot remove an ad that is served
from the same host as the chapter text, and it cannot stop a site from
showing an ad as plain text. It removes the ad elements and the tracking
requests it can recognise - not every ad.

### Listening player

The native TTS session is owned by `src/services/tts/TtsPlayerProvider.tsx`,
which sits above the navigator. The reader used to own it, which meant
playback stopped as soon as the reader was left.

- The queue is built from sentences, not paragraphs, in
  `assets/reader/js/core.js`. Every sentence keeps the element it came
  from, so the highlight follows the text while it is read.
- `src/screens/player` holds the full screen player, the chapter list and
  the speed, sleep timer and voice sheets.
- The sleep timer is implemented in `TtsPlaybackStore.kt`, so it keeps
  running while the app is in the background.
- `TtsMiniPlayer` shows a compact bar in the reader.

### Reader hardening

A chapter comes from the internet, so the reader assumes it can be broken.

- `src/screens/reader/utils/limits.ts` holds the limits: a 30 second load
  timeout, a 5 MB chapter size cap.
- `ReaderErrorBoundary` catches a failure while rendering the reader and
  shows a retry instead of letting it take the app down.
- The WebView reports a killed renderer, which triggers a reload of the
  chapter.
- Plugin payloads are parsed with `safeJsonParse`, so a broken repository
  list cannot crash the app.

## Deliberate limits

- No paywall or login bypass. The app only reads what the source serves
  without an account.
- The ad blocker is honest about what it does - see the description in
  the settings screen.
- iOS is not a build target. The iOS sources are kept in sync, but only
  the Android app is released.

## Next steps

- Publish the `v0.3.0` release once the CI secrets are configured.
- Test the player and the blocker on a device.
