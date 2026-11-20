# ``RemaplyMaps``

Show a MapLibre map in an iOS app with a few lines of code, in SwiftUI or UIKit.

## Overview

Remaply Maps wraps MapLibre Native iOS 7.0.0. You always choose the map style; the SDK never adds a
tile provider. The same API exists on Android with identical names and behavior.

```swift
import RemaplyMaps
import SwiftUI

struct ContentView: View {
    var body: some View {
        RemaplyMap(
            style: .url("https://example.com/style.json"),
            onMapReady: { controller in
                try? controller.addMarker(Marker(id: "home",
                    coordinate: GeoCoordinate(latitude: 48.8566, longitude: 2.3522)))
            }
        )
        .ignoresSafeArea()
    }
}
```

## Topics

### Essentials

- <doc:GettingStarted>
- ``RemaplyMap``
- ``RemaplyMapView``
- ``MapController``

### Style and options

- ``MapStyle``
- ``MapOptions``

### Camera

- ``CameraPosition``
- ``CameraUpdate``
- ``CameraChangeReason``
- ``GeoCoordinate``

### Markers

- ``Marker``
- ``MarkerIcon``
- ``MarkerAnchor``

### Events and lifecycle

- ``MapListener``
- ``MapLifecycleState``

### Errors and logging

- ``MapError``
- ``MapErrorCode``
- ``RemaplyMaps/RemaplyMaps``
- ``LogLevel``
- ``MapLogger``
