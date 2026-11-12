<p align="center">
  <a href="https://github.com/wuwenhou/universal-reader">
    <img src="./.github/readme-images/icon_new.png" align="center" width="128" />
  </a>
</p>

<h1 align="center">Universal Reader</h1>

<p align="center">
  Universal Reader is a free and open source light novel reader for Android,
  forked from LNReader.
</p>

<div align="center">
  <a href="https://github.com/wuwenhou/universal-reader/releases">
    <img alt="GitHub Downloads" src="https://img.shields.io/github/downloads/wuwenhou/universal-reader/total?label=downloads&labelColor=27303D&color=0D1117&logo=github&logoColor=FFFFFF&style=flat">
  </a>
</div>

<div align="center">
  <img alt="GitHub Actions Workflow Status" src="https://img.shields.io/github/actions/workflow/status/wuwenhou/universal-reader/build.yml?labelColor=27303D&style=flat">
  <a href="https://github.com/wuwenhou/universal-reader/blob/main/LICENSE">
    <img alt="GitHub" src="https://img.shields.io/github/license/wuwenhou/universal-reader?labelColor=27303D&color=1a73e8&style=flat">
  </a>
</div>

<h2 align="center">Download</h2>

<p align="center">
  <a href="https://github.com/wuwenhou/universal-reader/releases/latest">
    <img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/wuwenhou/universal-reader?label=Stable&labelColor=0d7377&color=084c4e&style=flat">
  </a>
</p>

<p align="center">
  Get the app from the <a href="https://github.com/wuwenhou/universal-reader/releases">releases page</a>.
</p>

<p align="center">
  <em>Android 7.0 or higher.</em>
</p>

## What this fork adds

- **Ad blocking** built on [uBO-compatible filter lists][ghostery]. EasyList,
  EasyPrivacy and ABPVN ship with the app and are applied to the chapter
  markup, to the requests a plugin makes and to the links the reader opens.
  Individual sites can be excluded in _Settings → Ad blocking_.
- **A full screen listening player** with a sentence level text-to-speech
  queue, a seek bar, a sleep timer, a speed and voice picker and a chapter
  list, so the narration keeps running while the app is used elsewhere.
- **A reader that survives bad chapters**: a chapter that fails to load, is
  too large or makes the WebView crash now shows an error with a retry
  button instead of closing the app.
- **HTTPS only** by default, with an explicit exception list for sources
  that have no HTTPS endpoint.

[ghostery]: https://github.com/ghostery/adblocker

## Screenshots

<p align="center">
  <img src="./.github/readme-images/screenshots.png" align="center" />
</p>

## Plugins

Universal Reader does not have any affiliation with the content providers
available.

Plugin requests should be created at
[lnreader-plugins](https://github.com/lnreader/lnreader-plugins).

## Translation

Help translate Universal Reader into your language on
[Crowdin](https://crowdin.com/project/lnreader).

## Building & Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md)

## License

[GPL-3.0](./LICENSE) — the fork is based on
[LNReader](https://github.com/LNReader/lnreader), which is MIT licensed, and
it bundles GPL-3.0 licensed filter lists. See [NOTICE](./NOTICE) for the
details.
