# Beta testing (0.1.x)

0.1.0 is the first beta. Everything in the [test report](test-report.md) ran on emulators and simulators; the beta
closes the gaps that need real devices and real apps. Fixes ship as 0.1.x patch releases
([releasing.md](releasing.md)).

## What the beta must answer

Each item comes from an open issue in the [test report](test-report.md#4-open-issues).

| # | Question | Test report issue | Devices |
|---|---|---|---|
| B1 | Does the SDK work on iOS 15.5–17? CI only runs iOS 26. | 1 | iPhone on iOS 15.5–16, iPhone on iOS 17 |
| B2 | Does MapLibre's per-map native memory growth (about 4.8 MB on the emulator) happen on a real device? | 2 | Any Android phone |
| B3 | Does everything that passes on emulators and simulators pass on physical hardware (GPU, touch, rotation)? | 4 | All |
| B4 | Do several map surfaces crash Android 7.x, as on the API 24 emulator, or is that the emulator's GL driver? | 5 | Android 7.0 or 7.1 phone |
| B5 | Do apps on the oldest supported toolchain build against the published artifacts? | — | Android: Kotlin 2.2 app. iOS: Xcode 16 app. |

## Device matrix

At least one physical Android device and one physical iPhone are required. Fill in who covers each row.

| Platform | Device | OS | Covers | Tester | Result |
|---|---|---|---|---|---|
| Android | Android 7.0–7.1 phone | API 24–25 | B3, B4 | | |
| Android | Recent phone (Pixel or Samsung) | API 34–36 | B2, B3 | | |
| Android | Phone without Vulkan support, if available | API 24–28 | B3 (OpenGL ES renderer) | | |
| iOS | iPhone | iOS 15.5–16 | B1, B3 | | |
| iOS | iPhone | iOS 17 | B1, B3 | | |
| iOS | iPhone | latest iOS | B3 | | |

## Checklist per device

Use the sample app from the release tag (`cd samples/android && ./gradlew installDebug`, or run
`samples/ios/RemaplySample.xcodeproj` on the device). For Android, also build it against the published artifacts
once they are public: `./gradlew -PremaplyFromMavenLocal=true installDebug` after
`(cd android && ./gradlew publishToMavenLocal)`, or point the sample at Maven Central.

| # | Step | Expected |
|---|---|---|
| 1 | Open **Basic map** | The map draws within a few seconds, with attribution visible |
| 2 | Pan, pinch-zoom, rotate, tilt | Gestures feel native; the compass appears after rotating |
| 3 | Open **Markers and taps**; tap each marker, then empty map | The panel names the tapped marker; an empty-map tap reports a map tap |
| 4 | Start a drag on a marker and slide off it | No marker tap |
| 5 | Open **Camera animation**; run each animation | Smooth movement; the completion is reported as finished |
| 6 | Start an animation and touch the map during it | The animation stops and reports not finished |
| 7 | Rotate the device on each screen | Camera, markers, and style are kept |
| 8 | Background the app for a minute, then return | The map resumes where it was |
| 9 | Android: enable **Don't keep activities**, background, return | The map comes back with the same style and camera |
| 10 | Turn on airplane mode, force-quit the app, open **Basic map** | "Could not load the style" with an error code; no crash |
| 11 | Open and close the samples 20 times | No crash; memory settles (Android Studio Profiler or Xcode memory gauge) |
| 12 | Open **Compose / SwiftUI** and **classic View / UIKit** examples | Same scenario and result on both |

B2: on Android, run the leak test on the device, which compares the SDK with a plain-MapLibre baseline:
`(cd android && ./gradlew :maps-android:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.remaply.maps.LeakTest)`.
Record both numbers it logs.

B5: follow the [Android](quickstart-android.md) and [iOS](quickstart-ios.md) quick starts in a new app on the
oldest supported Kotlin and Xcode versions, using the published artifacts.

## Reporting

Open a GitHub issue per problem with the label `beta`, and include:

- device model, OS version, and SDK version (`RemaplyMaps.version`)
- the checklist step, what you expected, and what happened
- logs: set `RemaplyMaps.logLevel` to `LogLevel.DEBUG` (Android) or `.debug` (iOS) and attach Logcat or the Xcode console; crash logs if any

Record each device's run in the matrix above and in [test-report.md](test-report.md) when the beta ends.

## Leaving the beta

The beta ends when:

- every row in the device matrix has a result, and B1–B5 are answered in the test report
- no open `beta` issue is a crash, a wrong event, or a behavior difference between Android and iOS
- any MapLibre problems found are reported upstream and linked from the test report
