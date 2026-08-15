<p align="center">
  <img src="https://raw.githubusercontent.com/Shrawan13-glitch/Mediyo/dev/app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp" width="96" alt="Mediyo" />
</p>

<h1 align="center">Mediyo</h1>

<p align="center">
  <strong>A Material 3 YouTube Music client for Android</strong>
</p>

<p align="center">
  <a href="https://github.com/Shrawan13-glitch/Mediyo/actions/workflows/debug_fast.yml"><img src="https://github.com/Shrawan13-glitch/Mediyo/actions/workflows/debug_fast.yml/badge.svg?branch=dev" alt="CI" /></a>
  <img src="https://img.shields.io/badge/version-1.1.2-blue" alt="Version" />
  <img src="https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android" alt="Android 7.0+" />
  <img src="https://img.shields.io/badge/license-GPL--3.0-blue" alt="License" />
  <img src="https://img.shields.io/badge/Material%20You-Yes-005eff" alt="Material You" />
</p>

---

## About

**Mediyo** is a free, open-source YouTube Music client built with **Kotlin** and **Jetpack Compose**.
It streams music directly from YouTube / YouTube Music, brings back **background playback**, and
couples it with a rich set of playback, discovery, and library tools — all wrapped in a modern
**Material 3** interface with dynamic color.

Based on [InnerTune](https://github.com/z-huang/InnerTune), Mediyo is community-driven, fully
self-hosted, and does not rely on any official YouTube API.

## Features

### Playback
- Stream from YouTube and YouTube Music
- Background playback with a rich notification and lock-screen controls
- Skip silence, audio normalization, and tempo/pitch adjustment
- Cache and download songs for fully offline playback

### Discovery
- Search songs, videos, albums, artists, and playlists
- Personalized **Quick Picks** and new release recommendations
- Moods & genres and YouTube Music browse feeds

### Library
- Streamlined library with virtual playlists (**Liked Music**, **Downloaded**)
- One-tap **Liked Music** sync for authenticated accounts
- Playback history and listening statistics

### Lyrics
- Synchronized lyrics with automatic source fallback
- Built-in lyrics translator

### Personalization & more
- Dynamic **Material You** theming and appearance settings
- Localized into dozens of languages
- Android Auto support
- Account login for your own recommendations
- Backup & restore of app data

> [!WARNING]
>
> If you're in a region where YouTube Music is not supported, you won't be able to use this app
> **unless** you have a proxy or VPN to connect to a YTM supported region.

## Screenshots

<p align="center">
  <img src="https://raw.githubusercontent.com/Shrawan13-glitch/Mediyo/dev/fastlane/metadata/android/en-US/images/phoneScreenshots/01.png" width="180" alt="Screenshot 1" />
  <img src="https://raw.githubusercontent.com/Shrawan13-glitch/Mediyo/dev/fastlane/metadata/android/en-US/images/phoneScreenshots/02.png" width="180" alt="Screenshot 2" />
  <img src="https://raw.githubusercontent.com/Shrawan13-glitch/Mediyo/dev/fastlane/metadata/android/en-US/images/phoneScreenshots/03.png" width="180" alt="Screenshot 3" />
  <img src="https://raw.githubusercontent.com/Shrawan13-glitch/Mediyo/dev/fastlane/metadata/android/en-US/images/phoneScreenshots/04.png" width="180" alt="Screenshot 4" />
  <img src="https://raw.githubusercontent.com/Shrawan13-glitch/Mediyo/dev/fastlane/metadata/android/en-US/images/phoneScreenshots/05.png" width="180" alt="Screenshot 5" />
</p>

## Build & Download

Mediyo ships in two flavors:

| Flavor | Description |
| ------ | ----------- |
| `foss` | No Google services — the default for privacy-conscious users |
| `full` | Includes Firebase, Crashlytics, and ML Kit translation support |

### From the source

```bash
# Debug build (FOSS flavor)
./gradlew assembleFossDebug

# Release build (FOSS flavor)
./gradlew assembleFossRelease

# Debug build (Full flavor)
./gradlew assembleFullDebug
```

Signed APKs from CI are attached to every run of the
[Fast Debug Build](https://github.com/Shrawan13-glitch/Mediyo/actions/workflows/debug_fast.yml)
workflow, and FOSS release APKs are published as GitHub Releases via the
[Build FOSS Release](https://github.com/Shrawan13-glitch/Mediyo/actions/workflows/foss_release.yml)
workflow.

### Requirements
- JDK 17
- Android SDK with build tools **35.0.0**

## Tech Stack

| Layer          | Technology                                        |
| -------------- | ------------------------------------------------- |
| Language       | Kotlin                                            |
| UI             | Jetpack Compose · Material 3                      |
| Media          | ExoPlayer (androidx.media3)                       |
| DI             | Hilt                                              |
| Persistence    | Room                                              |
| Networking     | Ktor Client · NewPipe Extractor (innertube)       |
| Imaging        | Coil                                              |
| Lyrics         | LrcLib · Kugou providers                          |
| Build          | Gradle (Kotlin DSL) · KSP / KAPT · R8             |

## FAQ

### How do I scrobble music to Last.fm, LibreFM, ListenBrainz, or GNU FM?

Use a scrobbler app such as [Pano Scrobbler](https://play.google.com/store/apps/details?id=com.arn.scrobble).

### Why isn't Mediyo showing in Android Auto?

1. Open Android Auto settings and tap the version repeatedly to enable developer settings.
2. Open the overflow menu (top-right) and select **Developer settings**.
3. Enable **Unknown sources**.

## Disclaimer

This project and its contents are not affiliated with, funded, authorized, endorsed by, or in any
way associated with YouTube, Google LLC, Innertune Media Inc., or any of their affiliates and
subsidiaries.

Any trademark, service mark, trade name, or other intellectual property rights used in this project
are the property of their respective owners.

## Credits & License

Mediyo is based on [InnerTune](https://github.com/z-huang/InnerTune), a Material 3 YouTube Music
client for Android. A heartfelt thank you to the original developer and all contributors.

This project is licensed under the [GPL-3.0 License](LICENSE).
