# Usage guide

How the SDK works, with Kotlin and Swift side by side. Names, parameters, defaults, and behavior are
the same on both platforms; the [API specification](../api-spec/API_SPEC.md) is the authoritative
reference.

## The pieces

| Piece | What it does |
|---|---|
| `RemaplyMap` (Compose, SwiftUI) or `RemaplyMapView` (View, UIKit) | Shows the map and delivers events. |
| `MapController` | Holds the style, camera, and markers, and is how you change them. It holds no reference to a view, so it's safe to keep in a `ViewModel` or `@StateObject`. |
| `MapListener` (or the callback parameters of `RemaplyMap`) | Receives events. |
| `RemaplyMaps` | SDK version and logging. |

## Style

The SDK never picks a tile provider; you always pass a style.

```kotlin
controller.setStyle(MapStyle.url("https://example.com/style.json"))   // http or https
controller.setStyle(MapStyle.bundled("styles/basic.json"))           // src/main/assets/styles/basic.json
```

```swift
controller.setStyle(.url("https://example.com/style.json"))
controller.setStyle(.bundled("styles/basic.json"))                  // app bundle, folder reference "styles"
```

`setStyle` never throws. Problems arrive in `onMapLoadError` with a code:

| Code | Cause | What the map shows |
|---|---|---|
| `INVALID_ARGUMENT` / `.invalidArgument` | Not an absolute http(s) URL, or a bad bundled path | The previous style stays, and markers keep working |
| `STYLE_NOT_FOUND` / `.styleNotFound` | The bundled file doesn't exist | The previous style stays |
| `STYLE_LOAD_FAILED` / `.styleLoadFailed` | Network or HTTP error, invalid JSON, TLS failure | No style until a later `setStyle` succeeds |

Markers survive style changes; the SDK re-adds them.

## Camera

```kotlin
controller.setCamera(CameraUpdate(center = GeoCoordinate(35.68, 139.77), zoom = 11.0))  // jump
controller.animateCamera(CameraUpdate(zoom = 14.0, bearing = 45.0), durationMs = 800) { finished ->
    // true: arrived. false: cancelled by another camera call or a touch on the map.
}
val camera = controller.camera
```

```swift
try controller.setCamera(CameraUpdate(center: GeoCoordinate(latitude: 35.68, longitude: 139.77), zoom: 11))
try controller.animateCamera(CameraUpdate(zoom: 14, bearing: 45), durationMs: 800) { finished in }
let camera = controller.camera
```

- A `null`/`nil` field in `CameraUpdate` keeps its current value.
- Zoom is clamped to `MapOptions.minZoom…maxZoom` (default 0–22), tilt to 0–60°, and bearing is normalized to 0–360°.
- An invalid center, or NaN or infinite numbers, throw `INVALID_ARGUMENT`. A call that throws changes nothing.
- Animations ease in and out and move in a straight line on screen. They behave the same on both platforms.

## Markers

```kotlin
controller.addMarker(Marker("cafe", GeoCoordinate(48.86, 2.34)))
controller.addMarker(Marker("logo", GeoCoordinate(48.85, 2.35), MarkerIcon.image(bitmap), MarkerAnchor.CENTER, zIndex = 1))
controller.updateMarker(controller.getMarker("cafe")!!.copy(zIndex = 5))
controller.removeMarker("cafe")   // false if it didn't exist
controller.removeAllMarkers()
```

```swift
try controller.addMarker(Marker(id: "cafe", coordinate: GeoCoordinate(latitude: 48.86, longitude: 2.34)))
try controller.addMarker(Marker(id: "logo", coordinate: c, icon: .image(uiImage), anchor: .center, zIndex: 1))
var cafe = controller.getMarker(id: "cafe")!
cafe.zIndex = 5
try controller.updateMarker(cafe)
controller.removeMarker(id: "cafe")
controller.removeAllMarkers()
```

- Ids must be unique and non-empty. Errors: `DUPLICATE_MARKER_ID`, `MARKER_NOT_FOUND`, `INVALID_ARGUMENT`.
- A higher `zIndex` draws on top; on ties, the most recently added or updated marker wins.
- Markers are drawn above all style layers, are never hidden by label collision, and stay upright when the map rotates.
- You can add markers before the map is ready; they appear once it is.

## Events

| Event | When |
|---|---|
| `onMapReady(controller)` | The first style loaded on this view. Fires again on a new view, such as after an Android rotation. |
| `onStyleLoaded(style)` | Every successful style load, after markers are re-added. |
| `onMapLoadError(error)` | A style request failed (see the table above). |
| `onMapTap(coordinate)` | A single tap that didn't hit a marker. |
| `onMarkerTap(marker)` | A single tap on a marker. Only the topmost marker is reported, and `onMapTap` doesn't fire. |
| `onCameraChange(camera, reason)` | The camera moved, at most once per frame. `reason` is `USER_GESTURE` or `PROGRAMMATIC`. |
| `onCameraIdle(camera)` | The camera stopped. Exactly one per movement. |

All events arrive on the main thread. If you call the controller from inside a callback, the events
that call causes arrive after your callback returns.

## Lifecycle and state

- **Lifecycle is automatic.** Android follows the Activity or Fragment lifecycle; iOS follows window
  attachment and app foreground/background. The manual methods (`onStart`, `onDestroy`, …) exist for
  unusual hosts and are safe to call more than once.
- **Rotation (Android).** Compose `rememberMapController()` and a `RemaplyMapView` with an id keep the
  same controller, so camera, style, and markers stay. `onMapReady` fires again on the new view.
- **Process death (Android).** Style and camera are restored. Markers are not, because they may hold
  bitmaps; re-add them in `onMapReady`, guarded with `getMarker(id) == null`.
- **Memory.** A destroyed view keeps nothing alive, and the controller only holds data. On iOS a view
  released without `onDestroy()` still frees its map.

## Logging and error reporting

```kotlin
RemaplyMaps.logLevel = LogLevel.WARN           // default
RemaplyMaps.logger = MapLogger { level, tag, message, error ->
    if (level >= LogLevel.WARN) crashReporter.log("$tag: $message", error)
}
```

```swift
final class Reporter: MapLogger, @unchecked Sendable {
    func log(level: LogLevel, tag: String, message: String, error: (any Error)?) {
        if level >= .warn { crashReporter.log("\(tag): \(message)") }
    }
}
RemaplyMaps.logger = Reporter()
```

Every SDK error passes through the logger. Messages from MapLibre itself arrive with tag `"MapLibre"`.
The logger may be called from any thread.

## Advanced: the native map (experimental)

`withNativeMap` gives you MapLibre's own objects (`MapLibreMap` and `MapView` on Android, `MLNMapView` on iOS)
while the map is ready. Use it for features v1 doesn't wrap, such as user location. It isn't covered by
semantic versioning or platform parity. Don't remove the SDK's marker layer or replace the style through it.
On Android, opt in with `@OptIn(ExperimentalRemaplyApi::class)`. If you enable location this way, declare the
location permissions in your own app manifest; the SDK removes MapLibre's.
