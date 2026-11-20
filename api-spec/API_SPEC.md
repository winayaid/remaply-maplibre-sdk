# Remaply Maps SDK — Public API Specification

| | |
|---|---|
| Spec version | 0.1.0 (draft, Phase 0) |
| Status | **Approved** (Phase 0). Clarified in Phases 2–4; see `CHANGELOG.md`. Parity verified: `docs/parity-checklist.md`. |
| Applies to | Android SDK (Kotlin) and iOS SDK (Swift) |
| Change control | Any change to this file after approval requires a matching `CHANGELOG.md` entry (see §17). |

This document defines every public type, method, parameter, callback, default value, and error case of the SDK. Both platform SDKs are implemented against this file. If an implementation and this file disagree, the implementation is wrong.

---

## Contents

1. [Platform baseline and pinned versions](#1-platform-baseline-and-pinned-versions)
2. [Packages and installation coordinates](#2-packages-and-installation-coordinates)
3. [Cross-platform conventions](#3-cross-platform-conventions)
4. [Architecture overview](#4-architecture-overview)
5. [Value types](#5-value-types)
6. [`MapController`](#6-mapcontroller)
7. [UI entry points](#7-ui-entry-points)
8. [Events](#8-events)
9. [Lifecycle and state retention](#9-lifecycle-and-state-retention)
10. [Errors](#10-errors)
11. [Logging and error reporting](#11-logging-and-error-reporting)
12. [Global SDK object: `RemaplyMaps`](#12-global-sdk-object-remaplymaps--api-080)
13. [Extension points](#13-extension-points)
14. [Behavior constants](#14-behavior-constants)
15. [Out of scope for v1](#15-out-of-scope-for-v1)
16. [Reference parity scenario](#16-reference-parity-scenario)
17. [Change control](#17-change-control)
18. [API item index](#18-api-item-index)
19. [Open questions for reviewers](#19-open-questions-for-reviewers)

---

## 1. Platform baseline and pinned versions

| | Android | iOS |
|---|---|---|
| Wrapped SDK | MapLibre Native Android | MapLibre Native iOS |
| Pinned version | **13.7.1** (released 2026-10-15) | **7.0.0** (released 2026-10-28) |
| Dependency | `org.maplibre.gl:android-sdk-opengl:13.7.1` (OpenGL ES renderer; the default `android-sdk` artifact uses Vulkan, which crashes on devices without Vulkan) | SPM `https://github.com/maplibre/maplibre-gl-native-distribution`, exact version `7.0.0`, product `MapLibre` |
| Minimum OS | **API 24** (Android 7.0). MapLibre requires API 23, so the requested minimum stands. | **iOS 15.5**. ⚠️ Raised from the requested iOS 15.0 because MapLibre iOS 7.0.0 requires 15.5. |
| Language | Kotlin 2.2+ (the SDK emits Kotlin 2.2 metadata; MapLibre 13.7.1 is built with Kotlin 2.3) | Swift 5.9+ (Xcode 16 or newer) |
| Compile SDK | **37**. Current AndroidX releases (core, Compose, Lifecycle) require apps to compile against API 37. This does not change the minimum OS. | n/a |
| UI toolkits | Jetpack Compose, classic `View` | SwiftUI, UIKit |

**Pinning rules**

- Versions are pinned exactly. No ranges, no `+`, no "up to next major".
- The Maven `latest` tag and the `main` branch of the iOS distribution repo currently point to `-globe` prerelease builds. Never resolve MapLibre by "latest".
- A MapLibre upgrade is a spec change (§17). It needs a migration note in `docs/` (Phase 6 provides the template).

---

## 2. Packages and installation coordinates

> Names in this section are proposals. See open question Q1.

| | Android | iOS |
|---|---|---|
| Core artifact / product | `com.remaply:maps-android:0.1.0` | Swift package at the repo root, product `RemaplyMaps` |
| Compose artifact | `com.remaply:maps-android-compose:0.1.0` (depends on the core artifact) | Not needed. SwiftUI support ships in `RemaplyMaps`. |
| Kotlin package / Swift module | `com.remaply.maps` (Compose: `com.remaply.maps.compose`) | `RemaplyMaps` |
| Repository | Maven Central, with a configurable private repo (Phase 7) | Git tag per version, for example `0.1.0` |

Compose is a separate Android artifact so that apps using only classic Views don't pull in Compose.

`Package.swift` must sit at the **repository root**, with sources under `ios/`, because SPM can only consume a package whose manifest is at the root of a Git repository.

### Dependencies

| Platform | Dependency | Why |
|---|---|---|
| Android | `org.maplibre.gl:android-sdk-opengl` (`api` scope) | The wrapped SDK. `api` scope lets the experimental escape hatch (§13.2) compile against MapLibre types. |
| Android | `androidx.lifecycle:lifecycle-runtime`, `lifecycle-viewmodel` | Automatic lifecycle binding and retaining state across rotation (§9). Already a transitive dependency of MapLibre and AppCompat. |
| Android (Compose artifact) | `androidx.compose.ui`, `androidx.compose.runtime`, `androidx.lifecycle:lifecycle-viewmodel-compose` | The Compose entry point |
| iOS | `MapLibre` | The wrapped SDK |

No other dependencies are allowed without a spec change.

---

## 3. Cross-platform conventions

### 3.1 Naming parity rules

1. **Type names are identical** on both platforms.
2. **Method and property names are identical.**
3. **Parameter names are identical.** A Swift argument label equals the Kotlin parameter name, with one exception: the first label is omitted (`_`) when the method name already ends with that parameter's noun. For example, Kotlin `addMarker(marker)` is Swift `addMarker(_ marker:)`. Every exception is shown in the signatures below.
4. **Enum cases** follow each platform's convention, with a mechanical mapping. Kotlin `UPPER_SNAKE_CASE` maps to Swift `lowerCamelCase` (`USER_GESTURE` ↔ `.userGesture`).
5. **Static factories have identical call sites**, for example `MapStyle.url("https://…")` on both platforms.

### 3.2 Type mapping

| Concept | Kotlin | Swift |
|---|---|---|
| Text | `String` | `String` |
| Floating-point number | `Double` | `Double` |
| Duration (always milliseconds) | `Long` | `Int` |
| Flag | `Boolean` | `Bool` |
| List | `List<T>` | `[T]` |
| Absent value | `T?` (`null`) | `T?` (`nil`) |
| Raster image | `android.graphics.Bitmap` | `UIImage` |
| Underlying error | `Throwable` | `Error` |
| Callback | Kotlin function type or interface | Swift closure or protocol |

### 3.3 Units

| Quantity | Unit |
|---|---|
| Latitude, longitude | Decimal degrees, WGS 84 |
| Zoom | MapLibre zoom level (0 = whole world) |
| Bearing | Degrees clockwise from true north, normalized to `[0, 360)` |
| Tilt | Degrees away from straight down (0 = top-down) |
| Duration | Milliseconds |
| Screen distance | dp (Android) ↔ pt (iOS), treated as the same unit |

### 3.4 Threading

- Every public API call must be made on the **main thread**, and every callback is delivered on the main thread. The one exception is `MapLogger` (§11), which may be called from any thread.
- How this is enforced differs by platform:
  - **Android:** methods carry `@MainThread`. A call from another thread throws `MapError(WRONG_THREAD)`.
  - **iOS:** public classes are `@MainActor`, so the compiler rejects calls from other threads.
- Correct code behaves the same on both platforms.

### 3.5 Errors from method calls

Methods that can reject their input are marked as throwing:

- **Kotlin:** `@Throws(MapError::class)`
- **Swift:** `throws`

The thrown type is `MapError` on both platforms (§10). A call that throws makes **no state change**.

---

## 4. Architecture overview

```
 App code
   │  creates / receives
   ▼
┌──────────────────────┐        binds to (one at a time)       ┌───────────────────────────────┐
│ MapController        │ ◄──────────────────────────────────── │ UI entry point                │
│ (state holder:       │                                       │  Android: RemaplyMapView,     │
│  style, camera,      │ ──── applies state to native map ───► │           RemaplyMap (Compose)│
│  markers)            │                                       │  iOS:     RemaplyMapView,     │
└──────────────────────┘                                       │           RemaplyMap (SwiftUI)│
                                                               └──────────────┬────────────────┘
                                                                              │ wraps
                                                                              ▼
                                                                MapLibre MapView / MLNMapView
                                                                              │
                                                     events (MapListener / closures) ──► App code
```

- **`MapController`** is the only object the app uses to change the map. It holds the *desired* style, camera, and markers. It never holds a strong reference to a View, a Context, or a UIView while it is not bound. This is why it is safe to keep in an Android `ViewModel` or a SwiftUI `@StateObject`.
- **UI entry points** own the native MapLibre view. They bind to a controller, push the controller's state into MapLibre, and deliver events to the app.
- **Events go to the UI entry point**, not the controller. A long-lived controller therefore can't leak short-lived listeners such as Activities or view controllers.

---

## 5. Value types

All value types are immutable on Android (`data class`) and value types on iOS (`struct`/`enum`, `Equatable`, `Sendable` where possible).

### 5.1 `GeoCoordinate` — API-001

```kotlin
data class GeoCoordinate(val latitude: Double, val longitude: Double) {
    val isValid: Boolean
}
```
```swift
public struct GeoCoordinate: Equatable, Hashable, Sendable {
    public var latitude: Double
    public var longitude: Double
    public init(latitude: Double, longitude: Double)
    public var isValid: Bool { get }
}
```

- `isValid` is true when both values are finite, `latitude ∈ [-90, 90]`, and `longitude ∈ [-180, 180]`.
- Construction never fails. Validation happens where the value is used. Passing an invalid coordinate to a throwing API throws `INVALID_ARGUMENT`.
- Coordinates the SDK delivers (taps, camera center) are always valid. Their longitude is wrapped into `[-180, 180]`.

### 5.2 `CameraPosition` — API-002

```kotlin
data class CameraPosition(
    val center: GeoCoordinate,
    val zoom: Double,
    val bearing: Double = 0.0,
    val tilt: Double = 0.0,
)
```
```swift
public struct CameraPosition: Equatable, Sendable {
    public var center: GeoCoordinate
    public var zoom: Double
    public var bearing: Double
    public var tilt: Double
    public init(center: GeoCoordinate, zoom: Double, bearing: Double = 0, tilt: Double = 0)
}
```

This is a complete camera. Positions the SDK reports are already clamped and normalized (§14).

### 5.3 `CameraUpdate` — API-003

```kotlin
data class CameraUpdate(
    val center: GeoCoordinate? = null,
    val zoom: Double? = null,
    val bearing: Double? = null,
    val tilt: Double? = null,
)
```
```swift
public struct CameraUpdate: Equatable, Sendable {
    public var center: GeoCoordinate?
    public var zoom: Double?
    public var bearing: Double?
    public var tilt: Double?
    public init(center: GeoCoordinate? = nil, zoom: Double? = nil,
                bearing: Double? = nil, tilt: Double? = nil)
}
```

- This is a partial camera change. A `null`/`nil` field keeps its current value.
- Validation, applied by the methods that accept an update:

| Condition | Result |
|---|---|
| `center` is not valid | Throws `INVALID_ARGUMENT` |
| `zoom`, `bearing`, or `tilt` is NaN or infinite | Throws `INVALID_ARGUMENT` |
| `zoom` is outside `[minZoom, maxZoom]` | Clamped silently |
| `tilt` is outside `[0, 60]` | Clamped silently |
| `bearing` is any finite value | Normalized to `[0, 360)` |
| All fields are `null` | No-op (see §6.3) |

### 5.4 `MapStyle` — API-004

```kotlin
sealed class MapStyle {
    data class Url(val url: String) : MapStyle()
    data class Bundled(val path: String) : MapStyle()
    companion object {
        fun url(url: String): MapStyle        // -> Url
        fun bundled(path: String): MapStyle   // -> Bundled
    }
}
```
```swift
public enum MapStyle: Equatable, Sendable {
    case url(String)
    case bundled(String)
}
```

Call sites are identical on both platforms: `MapStyle.url("https://example.com/style.json")` and `MapStyle.bundled("styles/basic.json")`.

| Kind | Meaning | Validation (reported via `onMapLoadError`, never thrown) |
|---|---|---|
| `url` | An absolute `http`/`https` URL to a MapLibre style JSON. The SDK never adds a default tile provider, API key, or headers. Keys go in the URL if the provider needs them. | Not parseable as absolute http(s) → `INVALID_ARGUMENT` |
| `bundled` | A path relative to the app's resources. Android resolves it in `assets/` (`styles/basic.json` → `src/main/assets/styles/basic.json`). iOS resolves it in `Bundle.main`'s resource directory, with folder structure kept (add the folder as a folder reference). | Empty path, or path containing `..` → `INVALID_ARGUMENT`. File missing → `STYLE_NOT_FOUND`. |

URLs inside a bundled style (sources, sprites, glyphs) must be absolute.

### 5.5 `MapOptions` — API-005

```kotlin
data class MapOptions(
    val initialCamera: CameraPosition? = null,
    val minZoom: Double = 0.0,
    val maxZoom: Double = 22.0,
    val scrollGesturesEnabled: Boolean = true,
    val zoomGesturesEnabled: Boolean = true,
    val rotateGesturesEnabled: Boolean = true,
    val tiltGesturesEnabled: Boolean = true,
    val compassEnabled: Boolean = true,
)
```
```swift
public struct MapOptions: Equatable, Sendable {
    public var initialCamera: CameraPosition?
    public var minZoom: Double
    public var maxZoom: Double
    public var scrollGesturesEnabled: Bool
    public var zoomGesturesEnabled: Bool
    public var rotateGesturesEnabled: Bool
    public var tiltGesturesEnabled: Bool
    public var compassEnabled: Bool
    public init(initialCamera: CameraPosition? = nil, minZoom: Double = 0, maxZoom: Double = 22,
                scrollGesturesEnabled: Bool = true, zoomGesturesEnabled: Bool = true,
                rotateGesturesEnabled: Bool = true, tiltGesturesEnabled: Bool = true,
                compassEnabled: Bool = true)
}
```

| Field | Behavior |
|---|---|
| `initialCamera` | Applied once, when a UI entry point binds to a controller that has no stored camera yet (§6.1). `null` means use the style's own `center`/`zoom`/`bearing`/`pitch`, or `(0, 0)` at zoom 0 if the style defines none. Ignored after creation. |
| `minZoom`, `maxZoom` | Limits for both gestures and API calls. They can't be passed to throwing code, so invalid values are corrected and logged at `WARN`: values outside `[0, 22]` are clamped, and if `minZoom > maxZoom` the two are swapped. |
| Gesture flags | `zoomGesturesEnabled` covers pinch, double-tap, and two-finger-tap zoom. Taps are delivered whatever these flags are set to. |
| `compassEnabled` | Shows the compass (top-right) while the bearing is not 0. Tapping it resets the bearing to 0. |

Every field except `initialCamera` can be changed at runtime (§7) and takes effect immediately.

The attribution control and the MapLibre logo are always visible and can't be configured in v1. This protects the data-provider attribution that tile licenses usually require (see `LICENSE`).

### 5.6 `Marker` — API-006

```kotlin
data class Marker(
    val id: String,
    val coordinate: GeoCoordinate,
    val icon: MarkerIcon = MarkerIcon.defaultPin(),
    val anchor: MarkerAnchor = MarkerAnchor.BOTTOM,
    val zIndex: Int = 0,
)
```
```swift
public struct Marker: Identifiable, Equatable {
    public var id: String
    public var coordinate: GeoCoordinate
    public var icon: MarkerIcon
    public var anchor: MarkerAnchor
    public var zIndex: Int
    public init(id: String, coordinate: GeoCoordinate, icon: MarkerIcon = .defaultPin(),
                anchor: MarkerAnchor = .bottom, zIndex: Int = 0)
}
```

| Field | Rules |
|---|---|
| `id` | Non-empty, and unique within a controller. The app chooses it and uses it to look up app-side data. |
| `coordinate` | Must be valid |
| `zIndex` | A higher value draws on top. On equal values, the most recently added or updated marker draws on top. |

**Rendering contract** (the same on both platforms):

- Markers are drawn above every style layer.
- They are never hidden by symbol collision, so every marker on screen is always visible.
- They don't rotate with the map and stay flat on screen when tilted.
- They survive style changes. The SDK re-adds them after every style load.

### 5.7 `MarkerIcon` — API-007

```kotlin
class MarkerIcon private constructor(/* internal */) {
    companion object {
        fun defaultPin(): MarkerIcon
        fun image(image: Bitmap): MarkerIcon
    }
}
```
```swift
public struct MarkerIcon: Equatable {
    public static func defaultPin() -> MarkerIcon
    public static func image(_ image: UIImage) -> MarkerIcon
}
```

- `defaultPin()` is an SDK-bundled pin, 28 × 40 dp/pt. The artwork is identical on both platforms and ships at 1x/2x/3x density.
- `image(...)` renders the image at its logical size:
  - **Android:** `bitmap.width / (bitmap.density / 160)` dp. Bitmaps with no density are treated as the device density.
  - **iOS:** `image.size` in points.
- Two icons are equal when they are the default pin, or when they wrap the same image instance. Images are registered with the style once per instance.

### 5.8 `MarkerAnchor` — API-008

The point of the icon that is placed at the marker's coordinate.

| Kotlin `enum class MarkerAnchor` | Swift `enum MarkerAnchor` |
|---|---|
| `CENTER`, `TOP`, `BOTTOM`, `LEFT`, `RIGHT`, `TOP_LEFT`, `TOP_RIGHT`, `BOTTOM_LEFT`, `BOTTOM_RIGHT` | `.center`, `.top`, `.bottom`, `.left`, `.right`, `.topLeft`, `.topRight`, `.bottomLeft`, `.bottomRight` |

### 5.9 `CameraChangeReason` — API-009

| Kotlin | Swift | Meaning |
|---|---|---|
| `USER_GESTURE` | `.userGesture` | The user moved the map: pan, pinch, rotate, tilt, double-tap, or a compass tap |
| `PROGRAMMATIC` | `.programmatic` | `setCamera`/`animateCamera`, or the camera being clamped after an options change |

### 5.10 `MapLifecycleState` — API-010

| Kotlin | Swift | Meaning |
|---|---|---|
| `INITIALIZED` | `.initialized` | Constructed, native map not created yet |
| `CREATED` | `.created` | Native map created, not visible or not rendering |
| `STARTED` | `.started` | Visible, rendering |
| `RESUMED` | `.resumed` | Visible, rendering, in the foreground and interactive |
| `DESTROYED` | `.destroyed` | Native resources released. Terminal state. |

---

## 6. `MapController`

The state holder and command interface. It is a class with reference semantics. On iOS it also conforms to `ObservableObject` so it can be held in `@StateObject`, but it publishes nothing.

```kotlin
class MapController() {
    val isMapReady: Boolean
    val style: MapStyle?
    val camera: CameraPosition?
    val markers: List<Marker>

    fun setStyle(style: MapStyle)
    @Throws(MapError::class) fun setCamera(update: CameraUpdate)
    @Throws(MapError::class) fun animateCamera(
        update: CameraUpdate,
        durationMs: Long = 500,
        completion: ((finished: Boolean) -> Unit)? = null,
    )

    fun getMarker(id: String): Marker?
    @Throws(MapError::class) fun addMarker(marker: Marker)
    @Throws(MapError::class) fun updateMarker(marker: Marker)
    fun removeMarker(id: String): Boolean
    fun removeAllMarkers()

    @ExperimentalRemaplyApi
    fun withNativeMap(block: (map: MapLibreMap, view: MapView) -> Unit): Boolean   // §13.2
}
```
```swift
@MainActor
public final class MapController: ObservableObject {
    public init()

    public var isMapReady: Bool { get }
    public var style: MapStyle? { get }
    public var camera: CameraPosition? { get }
    public var markers: [Marker] { get }

    public func setStyle(_ style: MapStyle)
    public func setCamera(_ update: CameraUpdate) throws
    public func animateCamera(_ update: CameraUpdate,
                              durationMs: Int = 500,
                              completion: ((_ finished: Bool) -> Void)? = nil) throws

    public func getMarker(id: String) -> Marker?
    public func addMarker(_ marker: Marker) throws
    public func updateMarker(_ marker: Marker) throws
    @discardableResult public func removeMarker(id: String) -> Bool
    public func removeAllMarkers()

    /// Experimental, see §13.2
    @discardableResult public func withNativeMap(_ block: (MLNMapView) -> Void) -> Bool
}
```

### 6.1 Properties — API-011 to API-014

| Property | Value |
|---|---|
| `isMapReady` (API-011) | `true` from just before `onMapReady` until the bound view is destroyed or unbound. It stays `true` while a later style loads. |
| `style` (API-012) | The style most recently requested via `setStyle` or a UI entry point, whether or not it has loaded. `null` before any request. |
| `camera` (API-013) | While a map is ready: the live camera. Otherwise: the stored camera, which is the last live camera, or the last value set while unbound. `null` if no camera was ever set or observed. |
| `markers` (API-014) | A snapshot of all markers, in insertion order (an update keeps a marker's position). |

### 6.2 `setStyle(style)` — API-015

- Requests a style load. It **never throws**. Every failure is reported through `onMapLoadError` (§8).
- Calling it with the style that is already current and loaded is a no-op and fires no events.
- If another load is in progress, it is abandoned and fires no events. Only the latest request reports success or failure.
- On success, all markers are re-added before `onStyleLoaded` fires.
- With no view bound, only the stored style changes. It loads when a view binds.

### 6.3 `setCamera(update)` — API-016

- Moves the camera immediately, with no animation.
- Throws `INVALID_ARGUMENT` per §5.3.
- Cancels any in-flight animation. That animation's `completion(false)` fires first.
- With a ready map, fires exactly one `onCameraChange(…, PROGRAMMATIC)` and then one `onCameraIdle`, unless the resulting camera equals the current one, in which case it fires no events.
- An empty update is a no-op and fires no events.
- With no ready map, it changes the stored camera and fires no events.

### 6.4 `animateCamera(update, durationMs, completion)` — API-017

- Animates from the current camera to the target with an **ease-in-out** curve (cubic-bezier 0.42, 0, 0.58, 1). The center moves along a straight line on screen; there is no fly-out arc.
- **Shared algorithm.** Both SDKs drive the animation themselves, one camera step per display frame, rather than using MapLibre's built-in animation. The center is interpolated linearly in Web Mercator space, taking the shorter way across the antimeridian. Zoom and tilt are interpolated linearly. The bearing takes the shorter arc.
- Throws `INVALID_ARGUMENT` per §5.3, or if `durationMs < 0`.
- `durationMs = 0` behaves like `setCamera`, then calls `completion(true)`.
- Fires `onCameraChange(…, PROGRAMMATIC)` at most once per frame, then `onCameraIdle`.
- `completion(true)` is called after the animation finishes, immediately after `onCameraIdle`.
- `completion(false)` is called if the animation is cancelled by any of the following:
  - another `setCamera`/`animateCamera` call,
  - any user touch on the map,
  - the view being unbound or destroyed.
- An empty update calls `completion(true)` immediately and fires no events.
- With no ready map, it changes the stored camera immediately with no animation and calls `completion(true)` synchronously. No closure is ever kept waiting for a map.
- `completion` is called exactly once.

### 6.5 Markers — API-018 to API-022

| Method | Success | Error cases |
|---|---|---|
| `getMarker(id)` (API-018) | Returns the marker, or `null`/`nil` | none |
| `addMarker(marker)` (API-019) | Adds and renders the marker (or renders it once a map is ready) | Empty `id` → `INVALID_ARGUMENT`. Invalid coordinate → `INVALID_ARGUMENT`. `id` already exists → `DUPLICATE_MARKER_ID`. |
| `updateMarker(marker)` (API-020) | Replaces the marker with the same `id`. Its draw order counts as "most recent" (§5.6). | No marker with that `id` → `MARKER_NOT_FOUND`. Invalid coordinate → `INVALID_ARGUMENT`. |
| `removeMarker(id)` (API-021) | Removes the marker and returns `true` | Unknown `id` returns `false` and is not an error |
| `removeAllMarkers()` (API-022) | Removes all markers | none |

- **Validation order** (identical on both platforms): empty `id` → invalid coordinate → duplicate / not found. For example, `addMarker` with an existing id *and* an invalid coordinate throws `INVALID_ARGUMENT`.
- Marker changes appear on the next rendered frame.
- Marker methods never fire camera events.
- There is no hard limit on marker count. v1 is performance-tested up to 1,000 markers.

### 6.6 Binding

- The controller binds automatically when passed to, or created by, a UI entry point. There is no public bind method.
- A controller is bound to **at most one view at a time**. If a second view binds while the first is still alive, the last binding wins: the first view is unbound, shows a blank map, and a `WARN` is logged.
- When a view is destroyed (§9), the controller is unbound. It keeps its style, camera, and markers, and it can be bound to a new view.

---

## 7. UI entry points

All four entry points take the same inputs: `style`, `options`, an optional `controller`, and the seven event callbacks from §8.

### 7.1 Android View: `RemaplyMapView` — API-030

```kotlin
class RemaplyMapView : FrameLayout {
    constructor(context: Context)
    constructor(context: Context, attrs: AttributeSet?)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
    constructor(
        context: Context,
        style: MapStyle?,
        options: MapOptions = MapOptions(),
        controller: MapController? = null,
    )

    val controller: MapController
    var options: MapOptions
    var listener: MapListener?
    val lifecycleState: MapLifecycleState

    // Manual lifecycle (§9). Not needed when a ViewTreeLifecycleOwner exists.
    fun onCreate(savedInstanceState: Bundle?)
    fun onStart()
    fun onResume()
    fun onPause()
    fun onStop()
    fun onDestroy()
    fun onLowMemory()
    fun onSaveInstanceState(outState: Bundle)   // Android-only, see §9.3
}
```

The XML attributes `app:remaply_styleUrl` and `app:remaply_styleAsset` set the initial style when inflating from XML. Apps that don't use XML don't need them.

### 7.2 Android Compose: `RemaplyMap` — API-031

```kotlin
@Composable
fun RemaplyMap(
    style: MapStyle,
    modifier: Modifier = Modifier,
    controller: MapController = rememberMapController(),
    options: MapOptions = MapOptions(),
    onMapReady: (MapController) -> Unit = {},
    onMapLoadError: (MapError) -> Unit = {},
    onStyleLoaded: (MapStyle) -> Unit = {},
    onMapTap: (GeoCoordinate) -> Unit = {},
    onMarkerTap: (Marker) -> Unit = {},
    onCameraChange: (CameraPosition, CameraChangeReason) -> Unit = { _, _ -> },
    onCameraIdle: (CameraPosition) -> Unit = {},
)

@Composable
fun rememberMapController(key: String? = null): MapController   // API-032
```

- `rememberMapController` keeps the controller in a `ViewModel` scoped to the current `ViewModelStoreOwner`. It therefore survives configuration changes such as rotation (§9.2).
- Use `key` to tell apart several maps on one screen.

### 7.3 iOS UIKit: `RemaplyMapView` — API-033

```swift
@MainActor
public final class RemaplyMapView: UIView {
    public init(frame: CGRect = .zero, style: MapStyle? = nil,
                options: MapOptions = MapOptions(), controller: MapController? = nil)
    public required init?(coder: NSCoder)

    public let controller: MapController
    public var options: MapOptions
    public weak var listener: MapListener?
    public var lifecycleState: MapLifecycleState { get }

    // Manual lifecycle (§9). Normally driven automatically.
    public func onCreate()
    public func onStart()
    public func onResume()
    public func onPause()
    public func onStop()
    public func onDestroy()
    public func onLowMemory()
}
```

Storyboard / `init(coder:)` instances start with no style. Call `controller.setStyle(_:)`.

### 7.4 iOS SwiftUI: `RemaplyMap` — API-034

```swift
public struct RemaplyMap: View {
    public init(style: MapStyle,
                controller: MapController? = nil,
                options: MapOptions = MapOptions(),
                onMapReady: ((MapController) -> Void)? = nil,
                onMapLoadError: ((MapError) -> Void)? = nil,
                onStyleLoaded: ((MapStyle) -> Void)? = nil,
                onMapTap: ((GeoCoordinate) -> Void)? = nil,
                onMarkerTap: ((Marker) -> Void)? = nil,
                onCameraChange: ((CameraPosition, CameraChangeReason) -> Void)? = nil,
                onCameraIdle: ((CameraPosition) -> Void)? = nil)
}
```

If `controller` is `nil`, the view creates one and keeps it for its lifetime, using `@StateObject` semantics. The Compose `modifier` has no SwiftUI parameter; use SwiftUI modifiers such as `.frame` and `.ignoresSafeArea` instead.

### 7.5 Shared input semantics — API-035

| Input | View classes (Android, UIKit) | Declarative (Compose, SwiftUI) |
|---|---|---|
| `style` | If not null, applied at construction. If null, the controller's stored style is used, if any. | The source of truth. When the value changes, `setStyle` is called. If the controller already has an equal style, nothing happens. |
| `options` | Read at construction. Setting the property later applies every field except `initialCamera`. | Changes on recomposition or update apply every field except `initialCamera`. |
| `controller` | If null, the view supplies one (Android: retained across rotation, see §9.2). | Defaults to a remembered controller. |
| Callbacks | `listener` | Closure parameters. The latest closures are always used. |

If a zoom-limit change in `options` pushes the current camera out of range, the camera is clamped and fires `onCameraChange(…, PROGRAMMATIC)` followed by `onCameraIdle`.

---

## 8. Events

### 8.1 `MapListener` — API-040

```kotlin
interface MapListener {
    fun onMapReady(controller: MapController) {}
    fun onMapLoadError(error: MapError) {}
    fun onStyleLoaded(style: MapStyle) {}
    fun onMapTap(coordinate: GeoCoordinate) {}
    fun onMarkerTap(marker: Marker) {}
    fun onCameraChange(camera: CameraPosition, reason: CameraChangeReason) {}
    fun onCameraIdle(camera: CameraPosition) {}
}
```
```swift
@MainActor
public protocol MapListener: AnyObject {
    func onMapReady(controller: MapController)
    func onMapLoadError(error: MapError)
    func onStyleLoaded(style: MapStyle)
    func onMapTap(coordinate: GeoCoordinate)
    func onMarkerTap(marker: Marker)
    func onCameraChange(camera: CameraPosition, reason: CameraChangeReason)
    func onCameraIdle(camera: CameraPosition)
}
// Every method has an empty default implementation in a protocol extension.
```

How the listener is referenced differs by platform:

- **Android:** a strong reference, cleared in `onDestroy`.
- **iOS:** a `weak` reference, the usual delegate pattern, which avoids a view → view-controller retain cycle.

### 8.2 Event semantics

| ID | Event | Fires when | Payload |
|---|---|---|---|
| API-041 | `onMapReady` | The first style has loaded on this view, stored markers are rendered, and the stored camera is applied. Fires **once per view instance**, so it fires again on a new view after rotation. | The bound controller |
| API-042 | `onMapLoadError` | A style request failed: an invalid style, a missing bundled file, a network or HTTP error, or style JSON that doesn't parse. Also fires if the renderer fails to start. If the *first* style fails, `onMapReady` doesn't fire until a later `setStyle` succeeds. Failures of individual tiles, sprites, or glyphs are **not** reported here; they are logged at `WARN`. | `MapError` with code `INVALID_ARGUMENT`, `STYLE_NOT_FOUND`, `STYLE_LOAD_FAILED`, or `RENDERER_INIT_FAILED` |
| API-043 | `onStyleLoaded` | Every successful style load, including the first, after markers are re-added | The style that loaded |
| API-044 | `onMapTap` | A confirmed single tap (not part of a double-tap) that doesn't hit a marker. Taps before `onMapReady` are ignored (this applies to `onMarkerTap` too). | Coordinate under the tap |
| API-045 | `onMarkerTap` | A confirmed single tap whose 8 dp/pt hit radius overlaps a rendered marker icon. If several markers overlap, only the topmost one (§5.6) is reported. The tap is consumed, so `onMapTap` does not fire. | The marker, as currently stored |
| API-046 | `onCameraChange` | The camera changed, at most once per rendered frame. Doesn't fire for the initial camera on first load. | Current camera and reason |
| API-047 | `onCameraIdle` | The camera stopped moving after one or more `onCameraChange` events | Final camera |

**What the map shows after a style error.** `INVALID_ARGUMENT` and `STYLE_NOT_FOUND` are detected before MapLibre is asked to load anything, so the previous style (if any) stays on screen with its markers, and marker changes keep rendering. `STYLE_LOAD_FAILED` means MapLibre already dropped the previous style, so the map shows no style until a later `setStyle` succeeds. Markers reappear with that style.

**Gesture physics** (for example, how far a fling coasts) follow each platform's MapLibre and are not part of the contract. The events are: `onCameraChange(…, USER_GESTURE)` while the map moves, then exactly one `onCameraIdle` when it stops.

### 8.3 Ordering guarantees

1. First load: `onStyleLoaded` → `onMapReady`. No camera events fire for the initial camera.
2. Every camera movement is `onCameraChange` (one or more), then exactly one `onCameraIdle`. A movement that starts before the previous idle continues the same sequence.
3. For an animation: … `onCameraChange` → `onCameraIdle` → `completion(true)`. For a cancelled animation: … `onCameraChange` → `onCameraIdle` → `completion(false)`, and then the events of whatever cancelled it. A cancelled animation always closes its movement with an idle (if the camera moved).
4. No event fires after `onDestroy` returns, or after the view is unbound.
5. Callbacks are never called again while one is still running (no re-entrancy). Calls the app makes to the controller from inside a callback are allowed. Events they cause are delivered after the current callback returns.

---

## 9. Lifecycle and state retention

### 9.1 Lifecycle state machine — API-050

`INITIALIZED → CREATED → STARTED → RESUMED`, back down through `STARTED → CREATED`, and finally `→ DESTROYED`.

- **Idempotent and order-tolerant.** A call that matches the current state does nothing. A call that skips states performs the missing ones in order, so `onResume()` from `CREATED` runs start and then resume. This makes it safe to combine automatic and manual lifecycle handling.
- **After `DESTROYED`.** Every lifecycle method and every view-level setter does nothing and logs a `WARN` with code `MAP_DESTROYED`.
- **`onDestroy()`** cancels animations with `completion(false)`, unbinds the controller, clears the listener and callbacks, and releases the native map and its renderer.
- **`onLowMemory()`** forwards to MapLibre to release caches. It has no effect on state.

**Automatic lifecycle (default)**

| Platform | Source |
|---|---|
| Android View | Binds to `findViewTreeLifecycleOwner()` when attached to a window. In Fragments this is the view lifecycle owner. If there is no owner, the app must call the lifecycle methods. `onLowMemory` comes from `ComponentCallbacks2`. |
| Compose | `LocalLifecycleOwner`. Leaving the composition calls `onDestroy`. |
| iOS UIKit | Moving into or out of a window → start/stop. `didEnterBackground` / `willEnterForeground` → pause+stop / start+resume. `didReceiveMemoryWarning` → `onLowMemory`. A view deallocated without `onDestroy` still releases its native map, and its controller keeps the last idle camera, style, and markers (the controller holds its view weakly). |
| SwiftUI | Same as UIKit. `dismantleUIView` calls `onDestroy`. |

### 9.2 Surviving rotation and backgrounding

| Scenario | Result (both platforms) |
|---|---|
| Backgrounding and returning | The same view instance is kept. Style, camera, and markers are unchanged. No `onMapReady`. |
| Rotation / configuration change | Style, camera, and markers are kept. |

On Android, rotation recreates the Activity, so the old view is destroyed and a new one binds the **same controller**. That controller is retained in a `ViewModel` by:

- `rememberMapController`, for Compose,
- the view itself, for `RemaplyMapView` with an id (`android:id` or `setId`),
- or the app, if it passed its own controller held in a `ViewModel`.

The new view fires `onMapReady` again. This is the one case where `onMapReady` repeats.

On iOS, rotation keeps the same view, so nothing is rebuilt.

**Android caveat.** A `RemaplyMapView` with no id and no app-supplied controller cannot retain its controller. Only the camera and style are restored, through saved state, and markers must be re-added in `onMapReady`. This matches the standard Android rule that views without an id don't save state. A debug-level log reports it.

### 9.3 Process death (Android) — API-051

- When the view has an id, `onSaveInstanceState(outState)` saves style and camera, and they are restored on recreation.
- Markers are **not** saved because they may hold Bitmaps. The app re-adds them in `onMapReady`.
- With automatic lifecycle this happens through the View's normal saved-state mechanism, and the manual method is only needed for custom hosts.
- **Compose:** `rememberMapController` saves style and camera with the screen's saved state and restores them into a fresh controller after process death. After a configuration change, the retained controller already has them and nothing is overwritten.
- iOS has no equivalent hook; app termination is handled by the app.

### 9.4 Memory guarantees

- After `onDestroy`, no SDK object holds a reference to the Activity, Context, View, UIView, view controller, or app listener.
- A controller with no bound view holds only value types and Bitmap/UIImage icons.
- 50 repeated create/destroy cycles must show no growth in retained objects. The instrumented and integration tests (Phase 5) check that every view, native map view, and Android Activity is released. They compare memory growth with plain MapLibre doing the same cycles in the same process; the SDK may add at most 16 MB. (MapLibre itself retains some native memory per map instance on the Android emulator; see `docs/test-report.md`.)

---

## 10. Errors

### 10.1 `MapError` — API-060

```kotlin
class MapError(
    val code: MapErrorCode,
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause)
```
```swift
public struct MapError: Error, CustomStringConvertible, Sendable {
    public let code: MapErrorCode
    public let message: String
    public let cause: (any Error)?
}
```

`code` is part of the contract. The `message` text is for humans only and may differ between platforms and versions.

### 10.2 `MapErrorCode` — API-061

| Kotlin | Swift | Raised by | Delivered via |
|---|---|---|---|
| `INVALID_ARGUMENT` | `.invalidArgument` | `setCamera`, `animateCamera`, `addMarker`, `updateMarker`. An invalid `MapStyle`. | Thrown. For style: `onMapLoadError`. |
| `DUPLICATE_MARKER_ID` | `.duplicateMarkerId` | `addMarker` | Thrown |
| `MARKER_NOT_FOUND` | `.markerNotFound` | `updateMarker` | Thrown |
| `STYLE_NOT_FOUND` | `.styleNotFound` | A bundled style file is missing | `onMapLoadError` |
| `STYLE_LOAD_FAILED` | `.styleLoadFailed` | Network or HTTP failure, or invalid style JSON | `onMapLoadError` |
| `RENDERER_INIT_FAILED` | `.rendererInitFailed` | The GPU surface or renderer couldn't start | `onMapLoadError` |
| `MAP_DESTROYED` | `.mapDestroyed` | View-level call after `onDestroy` | Logged at `WARN` only |
| `WRONG_THREAD` | `.wrongThread` | Call from a background thread (Android only, §3.4) | Thrown |
| `INTERNAL` | `.internal` | An unexpected MapLibre failure that is caught and contained | Logged at `ERROR`. `onMapLoadError` if it happens during a style load. |

Every error the SDK creates, whether thrown or delivered, is also passed to the logger (§11) at `WARN` or above.

---

## 11. Logging and error reporting

```kotlin
enum class LogLevel { VERBOSE, DEBUG, INFO, WARN, ERROR, NONE }

fun interface MapLogger {
    fun log(level: LogLevel, tag: String, message: String, error: Throwable?)
}
```
```swift
public enum LogLevel: Int, Comparable, Sendable {
    case verbose, debug, info, warn, error, none
}

public protocol MapLogger: AnyObject, Sendable {
    func log(level: LogLevel, tag: String, message: String, error: (any Error)?)
}
```

- **API-070** `LogLevel`, **API-071** `MapLogger`.
- `tag` is `"Remaply"` for SDK messages and `"MapLibre"` for messages forwarded from the wrapped SDK.
- Messages below `RemaplyMaps.logLevel` are dropped before the logger is called.
- The default logger (when `RemaplyMaps.logger` is null) writes to Logcat on Android and to `os.Logger` (subsystem `com.remaply.maps`) on iOS.
- **The logger may be called from any thread**, because MapLibre logs from worker threads. Implementations must be thread-safe.
- The logger is held strongly and globally. Don't capture an Activity or view controller in it.
- MapLibre iOS release binaries have no debug/verbose levels, so MapLibre's own messages at those levels exist only on Android. The SDK's own messages are identical on both platforms.
- This logger is the **error-reporting hook** required by Phase 4. Apps forward `WARN`/`ERROR` entries to their crash or analytics tool from here.

---

## 12. Global SDK object: `RemaplyMaps` — API-080

```kotlin
object RemaplyMaps {
    val version: String            // "0.1.0"
    val mapLibreVersion: String    // "13.7.1"
    var logLevel: LogLevel         // default WARN
    var logger: MapLogger?         // default null → platform logger
}
```
```swift
public enum RemaplyMaps {
    public static let version: String          // "0.1.0"
    public static let mapLibreVersion: String  // "7.0.0"
    public static var logLevel: LogLevel       // default .warn
    public static var logger: MapLogger?       // default nil → platform logger
}
```

- No initialization call is needed. On Android the SDK calls `MapLibre.getInstance(context)` on first use.
- `logLevel` and `logger` are thread-safe. They are protected internally by a lock, which is why they can be set from any thread, unlike the rest of the API.

---

## 13. Extension points

### 13.1 User location (not in v1) — API-090

v1 doesn't show user location. Two things are reserved for a later minor release:

- **Names:** `MapController.setUserLocationEnabled`, `MapOptions.userLocationEnabled`, and `MapListener.onUserLocationChange`. Apps must not rely on these names meaning anything in v1.
- **Responsibility:** permissions will stay the app's job; the SDK will never request them.

Until then, apps can enable MapLibre's location component through the escape hatch below.

**Permissions (Android).** The SDK removes the location permissions that MapLibre's manifest declares, so apps that don't use location don't request them. An app that enables location through the escape hatch declares `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` in its own manifest.

### 13.2 Native map escape hatch (experimental) — API-091

```kotlin
@ExperimentalRemaplyApi
fun withNativeMap(block: (map: MapLibreMap, view: MapView) -> Unit): Boolean
```
```swift
/// - Warning: Experimental. Not covered by semantic versioning or platform parity.
@discardableResult public func withNativeMap(_ block: (MLNMapView) -> Void) -> Bool
```

- Runs `block` synchronously with the native MapLibre objects and returns `true`, but only when a map is ready. Otherwise it returns `false` without running the block.
- **Excluded from parity.** The objects are MapLibre's own, which are different on each platform.
- Kotlin callers must opt in with `@OptIn(ExperimentalRemaplyApi::class)`.
- Apps must not keep references to the objects passed to `block`.
- Changes made through the escape hatch that conflict with the SDK are unsupported, for example removing the SDK's marker layer or replacing the style directly.

---

## 14. Behavior constants

| Constant | Value |
|---|---|
| Zoom range (default / hard limits) | `[0, 22]` / `[0, 22]` |
| Tilt range | `[0, 60]` degrees |
| Bearing normalization | `[0, 360)` |
| Default animation duration | 500 ms |
| Animation curve | Ease-in-out, cubic-bezier (0.42, 0, 0.58, 1) |
| Marker tap hit radius | 8 dp/pt around the touch point |
| `onCameraChange` max rate | Once per rendered frame |
| Default log level | `WARN` |
| Default pin size | 28 × 40 dp/pt, anchor `BOTTOM` |
| Control placement | Compass top-right. Attribution bottom-right. Logo bottom-left. All with 8 dp/pt margins. |
| Initial camera when none is given | The style's camera, else `(0, 0)` at zoom 0 |
| Supported style URL schemes | `http`, `https` (local files use `MapStyle.bundled`) |

---

## 15. Out of scope for v1

- User location display and tracking (reserved, §13.1)
- Routing, geocoding, offline tile packs, tile hosting, and any custom tile server
- Custom HTTP headers or authentication for style or tile requests
- Shapes (polylines, polygons, circles), info windows / callouts, marker dragging, clustering
- Declarative markers in Compose or SwiftUI (markers are imperative, via `MapController`)
- Long-press events, screen ↔ coordinate projection, querying rendered features
- Java-friendly overloads and an Objective-C API. Java callers can use the Kotlin API, but it's not covered by the parity guarantee.
- Maps inside scrolling lists

---

## 16. Reference parity scenario

Phase 4 runs this scenario on both platforms and the Phase 5 UI tests automate it. The observable results must match.

1. Create a map with `MapStyle.url(<demo style>)` and `initialCamera = CameraPosition(GeoCoordinate(48.8566, 2.3522), zoom = 12)`.
2. Expect `onStyleLoaded` then `onMapReady`, with no camera events.
3. Add markers `a` at (48.8584, 2.2945), `b` at (48.8606, 2.3376) with `zIndex = 1`, and `c` at (48.8530, 2.3499).
4. Call `animateCamera(CameraUpdate(zoom = 13, bearing = 45), 1000)`. Expect one or more `onCameraChange(PROGRAMMATIC)`, then `onCameraIdle` with bearing 45 and zoom 13, then `completion(true)`.
5. Tap marker `b` → `onMarkerTap(b)`, and `onMapTap` does not fire. Tap empty map → `onMapTap` with a coordinate within 0.0005° of the expected point.
6. Call `addMarker` with id `a` → throws `DUPLICATE_MARKER_ID`. Call `updateMarker` with id `zzz` → throws `MARKER_NOT_FOUND`. Call `removeMarker("zzz")` → returns `false`.
7. Call `setStyle(MapStyle.bundled("missing.json"))` → `onMapLoadError(STYLE_NOT_FOUND)`. Then call `setStyle(<demo style>)` → `onStyleLoaded`, and all 3 markers are still visible.
8. Rotate the device and send the app to the background and back. The camera and all 3 markers are unchanged.

The samples use a free, keyless demo style. Proposed: OpenFreeMap "Liberty" (`https://tiles.openfreemap.org/styles/liberty`), with `https://demotiles.maplibre.org/style.json` as a low-detail fallback. The SDK itself never references either one.

---

## 17. Change control

- After approval, any change to a public name, signature, default, error code, or behavior in this file requires:
  1. a pull request that edits this file,
  2. a `CHANGELOG.md` entry under `Unreleased`,
  3. updates to both platform implementations, or a parity-checklist item marked open.
- Versioning follows SemVer starting at 0.1.0. Before 1.0.0, breaking changes bump the minor version. From 1.0.0 on, they bump the major version.
- Items marked experimental (§13.2) are exempt from SemVer.

---

## 18. API item index

Phase 4's parity checklist and Phase 5's test report use these IDs.

| ID | Item | | ID | Item |
|---|---|---|---|---|
| API-001 | `GeoCoordinate` | | API-030 | `RemaplyMapView` (Android) |
| API-002 | `CameraPosition` | | API-031 | `RemaplyMap` (Compose) |
| API-003 | `CameraUpdate` + validation | | API-032 | `rememberMapController` |
| API-004 | `MapStyle` url/bundled | | API-033 | `RemaplyMapView` (UIKit) |
| API-005 | `MapOptions` + runtime changes | | API-034 | `RemaplyMap` (SwiftUI) |
| API-006 | `Marker` + rendering contract | | API-035 | Shared input semantics |
| API-007 | `MarkerIcon` | | API-040 | `MapListener` |
| API-008 | `MarkerAnchor` | | API-041 | `onMapReady` |
| API-009 | `CameraChangeReason` | | API-042 | `onMapLoadError` |
| API-010 | `MapLifecycleState` | | API-043 | `onStyleLoaded` |
| API-011 | `isMapReady` | | API-044 | `onMapTap` |
| API-012 | `style` | | API-045 | `onMarkerTap` |
| API-013 | `camera` | | API-046 | `onCameraChange` |
| API-014 | `markers` | | API-047 | `onCameraIdle` |
| API-015 | `setStyle` | | API-050 | Lifecycle state machine + auto lifecycle |
| API-016 | `setCamera` | | API-051 | State retention (rotation, background, process death) |
| API-017 | `animateCamera` | | API-060 | `MapError` |
| API-018 | `getMarker` | | API-061 | `MapErrorCode` |
| API-019 | `addMarker` | | API-070 | `LogLevel` |
| API-020 | `updateMarker` | | API-071 | `MapLogger` |
| API-021 | `removeMarker` | | API-080 | `RemaplyMaps` |
| API-022 | `removeAllMarkers` | | API-090 | User-location extension point (doc only) |
| | | | API-091 | `withNativeMap` (experimental, parity-exempt) |

---

## 19. Open questions for reviewers

Each question has a recommended answer. Approving the spec as written accepts all of them.

| # | Question | Recommendation |
|---|---|---|
| Q1 | Product and package names: `Remaply Maps`, Maven group `com.remaply`, Kotlin package `com.remaply.maps`, Swift module `RemaplyMaps`, view names `RemaplyMapView` / `RemaplyMap`. | Accept, or supply the final names now. Renaming after Phase 2 is expensive. |
| Q2 | iOS minimum is **15.5** (MapLibre 7.0.0) instead of 15.0. The alternative is MapLibre iOS 6.31.0, which keeps 15.0 but is the previous major. | Use 7.0.0 / iOS 15.5. The Swift API is unchanged from 6.x, and 15.5 covers almost the same devices. |
| Q3 | Markers are drawn as an SDK-managed GeoJSON source + symbol layer on both platforms, rather than with MapLibre Android's deprecated `Marker` annotations or iOS `MLNPointAnnotation` views. | Accept. It gives identical rendering, z-ordering, and hit-testing on both platforms. |
| Q4 | The Android-only public members are `RemaplyMapView.onSaveInstanceState(Bundle)`, the `Bundle?` parameter of `onCreate` (iOS `onCreate()` takes none), and the XML attributes. | Accept. They are idiomatic and only needed for manual-lifecycle hosts. |
| Q5 | Experimental `withNativeMap` escape hatch, which requires MapLibre as an Android `api` dependency. | Accept. It is the documented extension point for location and advanced use until v1.x adds first-class APIs. |
| Q6 | Compose ships as a separate Android artifact. | Accept |
| Q7 | Demo style for samples: OpenFreeMap Liberty, with MapLibre demotiles as fallback. | Accept. Confirm the OpenFreeMap terms fit your intended use of the samples. |
