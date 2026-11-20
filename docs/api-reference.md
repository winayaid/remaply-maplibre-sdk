# API reference

The API reference is generated from the code comments, so it always matches the code. Builds fail on
warnings, so every public declaration must be documented and every symbol link must resolve.

| Platform | Tool | Build command | Output |
|---|---|---|---|
| Android | Dokka (HTML) | `cd android && ./gradlew :dokkaGenerate` | `android/build/dokka/html/index.html` |
| iOS | DocC | `xcodebuild docbuild -scheme RemaplyMaps -destination 'generic/platform=iOS Simulator' -derivedDataPath build/docc` | `build/docc/Build/Products/Debug-iphonesimulator/RemaplyMaps.doccarchive` (open in Xcode) |

In Xcode you can also choose **Product › Build Documentation** with the `RemaplyMaps` scheme.

CI builds both on every pull request (the `docs.yml` workflow) and uploads them as artifacts.

## Sources

- Android: KDoc in `android/maps/src/main/kotlin` and `android/maps-compose/src/main/kotlin`, plus the module
  pages `android/maps/Module.md` and `android/maps-compose/Module.md`.
- iOS: DocC comments in `ios/Sources/RemaplyMaps`, plus the catalog `ios/Sources/RemaplyMaps/RemaplyMaps.docc`
  (landing page and the Getting Started article).

The [API specification](../api-spec/API_SPEC.md) describes the contract that both references document.
