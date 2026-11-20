# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
Any change to the public API must update [`api-spec/API_SPEC.md`](api-spec/API_SPEC.md)
and add an entry here (API_SPEC.md §17).

## [Unreleased]

## [0.0.1] - 2026-11-20

Trial release to test publishing to Maven Central and SPM. Android artifacts are
`io.github.winayaid:remaply-maps-trial-android` and `remaply-maps-trial-compose`; the final SDK will use other
coordinates. See [docs/beta-testing.md](docs/beta-testing.md).

### Added

- Public API specification for both platforms: `api-spec/API_SPEC.md` (approved in Phase 0).
- Repository layout: `api-spec/`, `android/`, `ios/`, `samples/android/`, `samples/ios/`, `docs/`.
- Android SDK build (`io.github.winayaid:remaply-maps-trial-android`, `io.github.winayaid:remaply-maps-trial-compose`) wrapping
  MapLibre Native Android 13.7.1. Minimum API 24, compileSdk 37.
- iOS Swift package (`RemaplyMaps`) wrapping MapLibre Native iOS 7.0.0. Minimum iOS 15.5.
- `RemaplyMaps.version` and `RemaplyMaps.mapLibreVersion` on both platforms.
- Foundation sample apps for Android and iOS that display a map with a demo style.
- CI for both platforms: build, unit tests, and lint on every pull request.
- BSD 2-Clause license with MapLibre notices and attribution requirements.
- **Android SDK (Phase 2):** full v1 API per `API_SPEC.md`.
  - `MapController`: style loading, `setCamera` / `animateCamera`, markers, `withNativeMap` (experimental).
  - `RemaplyMapView` (classic View, XML attributes) and `RemaplyMap` / `rememberMapController` (Compose).
  - Automatic lifecycle via `LifecycleOwner`; controller and markers survive rotation.
  - Markers rendered as an SDK-managed GeoJSON source + symbol layer with an 8 dp tap radius.
  - Logging and error reporting: `RemaplyMaps.logLevel`, `RemaplyMaps.logger`, MapLibre logs forwarded.
- Android sample: Compose and classic View screens running the reference scenario (API_SPEC.md §16).
- **iOS SDK (Phase 3):** full v1 API per `API_SPEC.md`, with the same names, defaults, error codes,
  event order, animation algorithm, and marker rendering as Android.
  - `MapController` (`ObservableObject`), `RemaplyMapView` (UIKit), `RemaplyMap` (SwiftUI).
  - Automatic lifecycle from window attachment and app foreground/background notifications.
  - Logging hook (`RemaplyMaps.logLevel`, `RemaplyMaps.logger`), MapLibre logs forwarded; default `os.Logger`.
- iOS sample: SwiftUI and UIKit screens running the reference scenario, plus a bundled demo style.

- **Parity (Phase 4):** `docs/parity-checklist.md` lists all 45 API items, verified on both platforms.
  - `scripts/check-api-parity.py` compares public API names (types, members, enum cases, parameters) and runs in CI.
  - `scripts/run-parity-trace.sh` runs the same scripted scenario on both samples and compares the event
    trace with `docs/parity/expected-trace.txt`, including a render probe of the markers actually drawn.

- **Tests (Phase 5):** see `docs/test-report.md`.
  - Android instrumented tests (style, marker, camera, taps, touch-cancel, errors, recreation, saved state,
    lifecycle, and leak checks), with HTTP stubbed in-process. CI runs them on API 24 and API 36 emulators.
  - iOS integration and leak tests in the package test target, and XCUITests in the sample (taps,
    touch-cancel, rotation) run on every pull request.

- **Documentation and samples (Phase 6):**
  - Quick starts for Android and iOS (each verified by building a fresh app from the doc), a usage guide
    with Kotlin and Swift side by side, an attribution guide, and a MapLibre upgrade template.
  - API reference generated with Dokka (Android) and DocC (iOS); both builds fail on warnings.
  - Samples restructured into a launcher with five screens on both platforms: basic map, markers and taps,
    camera animation, Compose/SwiftUI example, classic View/UIKit example. UI tests open every screen.
  - CI: `docs.yml` (link check, Dokka, DocC) and the sample-screen tests.
- **Release tooling (Phase 7):** see `docs/releasing.md` and `docs/beta-testing.md`.
  - Android publishing with full POM metadata, sources, and Dokka javadoc: Maven Central (Central Portal,
    released by hand after review), and an optional private Maven repo set by `remaplyPrivateRepoUrl`.
    Signing is enabled when `signingInMemoryKey` is provided.
  - `release.yml`: on a version tag, checks the tag, versions, and changelog; signs and uploads the Android
    artifacts after approval of the `release` environment; checks that the iOS package resolves from the tag;
    and drafts a GitHub release.
  - Every pull request now publishes to Maven Local, checks the artifacts (`scripts/check-publication.py`),
    builds the Android sample against them, and builds an SPM consumer of a locally tagged copy
    (`scripts/check-spm-consumer.sh`) that compiles the iOS quick start code.
  - Beta test plan for 0.1.x: device matrix and per-device checklist covering the test report's open issues.
- Android: the SDK removes MapLibre's `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` permissions from apps,
  because v1 doesn't use location.

### Changed (spec clarifications, Phase 2)

These make existing behavior explicit; no public names or signatures changed.

- §1: Android apps must compile against API 37 (required by current AndroidX). Minimum OS is unchanged.
- §6.4: Both SDKs drive camera animations frame by frame with the same interpolation algorithm.
- §6.5: Marker validation order is empty id → invalid coordinate → duplicate / not found.
- §8.2: Taps before `onMapReady` are ignored.
- §8.3: A cancelled animation emits `onCameraIdle` before `completion(false)`.
- §9.1 (Phase 3): On iOS, a view deallocated without `onDestroy` releases its map; the controller keeps its state.
- §8.2 (Phase 4): What the map shows after each kind of style error; gesture physics are platform-native.
- §11 (Phase 4): MapLibre iOS has no debug/verbose levels for its own messages.
- §9.3 (Phase 5): Compose `rememberMapController` also saves style and camera for process death.
- §9.4 (Phase 5): Leak checks measure SDK overhead against a plain-MapLibre baseline.
- §13.1 (Phase 6): The SDK removes MapLibre's location permissions; apps using location declare them.
- §2 (release): The trial publishes as `io.github.winayaid:remaply-maps-trial-android` and
  `io.github.winayaid:remaply-maps-trial-compose` instead of the proposed `com.remaply:maps-android*`, with the
  Kotlin package `io.github.winayaid.mapstrial` (Compose: `.compose`) instead of `com.remaply.maps`, so the trial
  never overlaps the final SDK. The Swift module is unchanged.

### Fixed

- iOS CI: create `build/` before writing test logs (tests passed but the job failed on a fresh checkout).
- iOS CI: pin XcodeGen 2.47.0; the Homebrew build on the runner generated a slightly different sample project.
- Android + iOS: after a style request rejected with `INVALID_ARGUMENT` or `STYLE_NOT_FOUND`, marker changes
  stopped rendering until another style loaded. Markers now render into whichever style is on screen.
- Android: a marker change could intermittently fail to render because MapLibre applied GeoJSON updates on a
  worker thread. The marker source now updates synchronously.
- Android: the SDK depended on `org.maplibre.gl:android-sdk`, which uses MapLibre's Vulkan renderer and crashes
  the app on devices without Vulkan (found by the API 24 CI emulator). It now uses `android-sdk-opengl`, the
  OpenGL ES renderer that the spec (§1) already named.
- Android Compose: after process death, a map using `rememberMapController` came back at its initial camera
  and without its style. Style and camera are now saved with the screen state.
