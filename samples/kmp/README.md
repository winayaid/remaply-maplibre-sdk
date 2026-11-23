# Kotlin Multiplatform sample

One Compose Multiplatform screen, shared by Android and iOS, that shows a Remaply map with four markers,
city buttons that fly the camera, and a status line for map and marker taps.

It uses the SDK the way a KMP app does:

| | Android | iOS |
|---|---|---|
| SDK from | Maven Central: `io.github.winayaid:remaply-maps-trial-android` and `-compose` `0.0.1` | Swift Package Manager: this repository at tag `0.0.1` |
| Map in the shared UI | `RemaplyMap` (Compose), called directly from `androidMain` | `RemaplyMapView` (UIKit), created in Swift and shown with `UIKitView` |

## Why iOS needs a Swift bridge

Kotlin/Native can only call Objective-C APIs, and the Remaply iOS SDK is a Swift API. So `iosMain` declares
three small interfaces, and the iOS app implements them in Swift:

```
shared (Kotlin)                                   iosApp (Swift)
---------------------------------------------     ------------------------------------------------
App()  ->  PlatformMap()  [expect]
           |- androidMain: RemaplyMap (SDK)
           '- iosMain: UIKitView(nativeMap.view)
                  |  NativeMapFactory.createMap  ->  RemaplyNativeMapFactory
                  |  NativeMap.flyTo / .view     ->  RemaplyNativeMap  (wraps RemaplyMapView)
                  '  NativeMapListener           <-  onMapReady / onMarkerTap / onMapTap
```

| File | What it does |
|---|---|
| [`shared/src/commonMain/.../App.kt`](shared/src/commonMain/kotlin/io/github/winayaid/mapstrial/kmp/App.kt) | The shared screen, the places, and `expect fun PlatformMap` |
| [`shared/src/androidMain/.../PlatformMap.android.kt`](shared/src/androidMain/kotlin/io/github/winayaid/mapstrial/kmp/PlatformMap.android.kt) | Android map: the SDK's Compose `RemaplyMap` |
| [`shared/src/iosMain/.../NativeMap.kt`](shared/src/iosMain/kotlin/io/github/winayaid/mapstrial/kmp/NativeMap.kt) | The bridge interfaces Swift implements |
| [`shared/src/iosMain/.../PlatformMap.ios.kt`](shared/src/iosMain/kotlin/io/github/winayaid/mapstrial/kmp/PlatformMap.ios.kt) | iOS map: shows the Swift view with `UIKitView` |
| [`shared/src/iosMain/.../MainViewController.kt`](shared/src/iosMain/kotlin/io/github/winayaid/mapstrial/kmp/MainViewController.kt) | iOS entry point; takes the Swift factory |
| [`iosApp/iosApp/RemaplyNativeMap.swift`](iosApp/iosApp/RemaplyNativeMap.swift) | The Swift bridge, built on `RemaplyMapView` |
| [`iosApp/iosApp/iOSApp.swift`](iosApp/iosApp/iOSApp.swift) | SwiftUI app that hosts the shared UI |

To expose more of the SDK to shared code (markers that change, camera events, styles), add members to the
bridge interfaces and implement them in `RemaplyNativeMap.swift`.

## Run it

**Android:** `./gradlew :androidApp:installDebug`, or open `samples/kmp` in Android Studio and run `androidApp`.

**iOS:** open `iosApp/iosApp.xcodeproj` in Xcode and run the `iosApp` scheme. A build step runs
`./gradlew :shared:embedAndSignAppleFrameworkForXcode` to build the Kotlin `Shared` framework; Xcode resolves
the SDK package on first open.

The Xcode project is generated from [`iosApp/project.yml`](iosApp/project.yml) with
[XcodeGen](https://github.com/yonaskolb/XcodeGen): after editing it, run `cd iosApp && xcodegen`.

To try SDK changes that aren't released yet on Android, publish them locally and build with
`-PremaplyFromMavenLocal=true`:

```bash
(cd ../../android && ./gradlew publishToMavenLocal)
./gradlew -PremaplyFromMavenLocal=true :androidApp:installDebug
```

## Notes

- `RemaplyNativeMap` is `@MainActor` because `RemaplyMapView` is a UIKit view; Compose calls the bridge on the
  main thread.
- Compose Multiplatform requires `CADisableMinimumFrameDurationOnPhone` in the app's `Info.plist` and stops at
  launch without it; `project.yml` sets it.
- On Android, `rememberMapController` keeps the camera and markers across rotation, and calls `onMapReady`
  again for the new view. The shared screen keeps its status in `rememberSaveable`.
