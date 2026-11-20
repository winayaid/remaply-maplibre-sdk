# Remaply Maps SDK

Native SDKs for showing a [MapLibre](https://maplibre.org) map in Android and iOS apps with a few
lines of code. Both platforms share the same API names, parameters, events, and behavior.

> **Status:** beta. 0.1.0 is the first release; see the [CHANGELOG](CHANGELOG.md) and
> [beta testing](docs/beta-testing.md).

**Get started:** [Android quick start](docs/quickstart-android.md) · [iOS quick start](docs/quickstart-ios.md) ·
[Usage guide](docs/guide.md) · [All docs](docs/README.md)

```kotlin
// Android (Compose)
RemaplyMap(style = MapStyle.url("https://example.com/style.json"), modifier = Modifier.fillMaxSize())
```

```swift
// iOS (SwiftUI)
RemaplyMap(style: .url("https://example.com/style.json")).ignoresSafeArea()
```

| | Android | iOS |
|---|---|---|
| Wraps | MapLibre Native Android **13.7.1** (OpenGL ES) | MapLibre Native iOS **7.0.0** |
| Minimum OS | API 24 (Android 7.0) | iOS 15.5 |
| UI toolkits | Jetpack Compose, classic View | SwiftUI, UIKit |
| Language | Kotlin 2.2+ | Swift 5.9+ (Xcode 16+) |

The SDK never includes a tile provider. You always pass a style URL, or a style file bundled in
your app.

## Repository layout

| Path | Contents |
|---|---|
| [`api-spec/`](api-spec/) | The public API spec. Both platforms implement against it. |
| [`android/`](android/) | Android SDK (Gradle): `maps` (core) and `maps-compose` |
| [`ios/`](ios/) | iOS SDK sources and tests. The Swift package manifest is [`Package.swift`](Package.swift) at the repo root. |
| [`samples/android/`](samples/android/) | Android sample app (consumes the SDK through a Gradle composite build) |
| [`samples/ios/`](samples/ios/) | iOS sample app (XcodeGen `project.yml` + generated Xcode project) |
| [`docs/`](docs/) | Guides and API reference |
| [`scripts/`](scripts/) | CI and maintenance scripts |

## Building locally

**Requirements:** JDK 17+, Android SDK (AGP downloads platform 37 automatically), Xcode 16 or newer.
For the iOS sample only: `brew install xcodegen swiftlint`.

```bash
# Android SDK: unit tests, Android Lint, ktlint
cd android && ./gradlew check

# Android sample
cd samples/android && ./gradlew installDebug

# iOS SDK: unit tests
xcodebuild test -scheme RemaplyMaps -destination "$(scripts/ios-sim-destination.sh)"

# iOS sample: open in Xcode and run
open samples/ios/RemaplySample.xcodeproj

# Lint and consistency checks
swiftlint lint --strict
scripts/check-versions.sh
python3 scripts/check-api-parity.py
python3 scripts/check-doc-links.py

# API reference (see docs/api-reference.md)
cd android && ./gradlew :dokkaGenerate
xcodebuild docbuild -scheme RemaplyMaps -destination 'generic/platform=iOS Simulator'
```

After editing `samples/ios/project.yml`, run `cd samples/ios && xcodegen` and commit the regenerated
project. CI fails if the two are out of sync.

## Contributing

- The public API is frozen by the spec. Changing it requires updating `api-spec/API_SPEC.md` and
  `CHANGELOG.md` in the same pull request.
- CI runs build, unit tests, and lint for both platforms on every pull request.
- Versions follow [SemVer](https://semver.org). The SDK version lives in
  `android/gradle.properties` and `ios/Sources/RemaplyMaps/RemaplyMaps.swift`, and
  `scripts/check-versions.sh` keeps them in sync.

## License

BSD 2-Clause. See [`LICENSE`](LICENSE). It also includes MapLibre Native's license and the
attribution requirements for apps that ship this SDK; see [docs/attribution.md](docs/attribution.md).
