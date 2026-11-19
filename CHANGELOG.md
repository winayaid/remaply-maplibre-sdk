# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
Any change to the public API must update [`api-spec/API_SPEC.md`](api-spec/API_SPEC.md)
and add an entry here (API_SPEC.md §17).

## [Unreleased]

Targeting **0.1.0** (first beta).

### Added

- Public API specification for both platforms: `api-spec/API_SPEC.md` (approved in Phase 0).
- Repository layout: `api-spec/`, `android/`, `ios/`, `samples/android/`, `samples/ios/`, `docs/`.
- Android SDK build (`com.remaply:maps-android`, `com.remaply:maps-android-compose`) wrapping
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

### Changed (spec clarifications, Phase 2)

These make existing behavior explicit; no public names or signatures changed.

- §1: Android apps must compile against API 37 (required by current AndroidX). Minimum OS is unchanged.
- §6.4: Both SDKs drive camera animations frame by frame with the same interpolation algorithm.
- §6.5: Marker validation order is empty id → invalid coordinate → duplicate / not found.
- §8.2: Taps before `onMapReady` are ignored.
- §8.3: A cancelled animation emits `onCameraIdle` before `completion(false)`.

### Fixed

- iOS CI: create `build/` before writing test logs (tests passed but the job failed on a fresh checkout).
