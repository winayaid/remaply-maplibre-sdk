# Documentation

## Start here

- [Quick start: Android](quickstart-android.md)
- [Quick start: iOS](quickstart-ios.md)
- [Usage guide](guide.md): style, camera, markers, events, lifecycle, logging, with Kotlin and Swift side by side
- [Attribution and licensing](attribution.md): what your app must show and include

## Reference

- [API reference](api-reference.md): generated from code comments (Dokka and DocC)
- [API specification](../api-spec/API_SPEC.md): the contract both platforms implement

## Samples

Both sample apps open a launcher with the same five samples:

| Sample | Android | iOS |
|---|---|---|
| Basic map | [`BasicMapScreen.kt`](../samples/android/app/src/main/kotlin/com/remaply/maps/sample/BasicMapScreen.kt) | [`BasicMapView.swift`](../samples/ios/RemaplySample/BasicMapView.swift) |
| Markers and taps | [`MarkersAndTapsScreen.kt`](../samples/android/app/src/main/kotlin/com/remaply/maps/sample/MarkersAndTapsScreen.kt) | [`MarkersAndTapsView.swift`](../samples/ios/RemaplySample/MarkersAndTapsView.swift) |
| Camera animation | [`CameraAnimationScreen.kt`](../samples/android/app/src/main/kotlin/com/remaply/maps/sample/CameraAnimationScreen.kt) | [`CameraAnimationView.swift`](../samples/ios/RemaplySample/CameraAnimationView.swift) |
| Compose / SwiftUI example | [`FullScenarioScreen.kt`](../samples/android/app/src/main/kotlin/com/remaply/maps/sample/FullScenarioScreen.kt) | [`RemaplySampleApp.swift`](../samples/ios/RemaplySample/RemaplySampleApp.swift) |
| Classic View / UIKit example | [`ViewSampleActivity.kt`](../samples/android/app/src/main/kotlin/com/remaply/maps/sample/ViewSampleActivity.kt) | [`UIKitSampleViewController.swift`](../samples/ios/RemaplySample/UIKitSampleViewController.swift) |

Run them with `cd samples/android && ./gradlew installDebug` and `open samples/ios/RemaplySample.xcodeproj`.

## Quality and process

- [Test report](test-report.md): Phase 5 results, manual test notes, and open issues
- [Parity checklist](parity-checklist.md): every API item verified on Android and iOS
- [MapLibre upgrade template](maplibre-upgrade-template.md): checklist for every MapLibre version change
- [Releasing](releasing.md): publishing to Maven Central, a private Maven repo, and SPM tags
- [Beta testing](beta-testing.md): the beta device matrix and checklist
