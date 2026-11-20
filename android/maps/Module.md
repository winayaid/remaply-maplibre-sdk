# Module remaply-maps-trial-android

Show a MapLibre map in an Android app with a few lines of code: classic Views and the shared core.

The SDK wraps MapLibre Native Android 13.7.1. You always choose the map style; the SDK never adds a
tile provider. Jetpack Compose support is in the `remaply-maps-trial-compose` module.

## Where to start

- [io.github.winayaid.mapstrial.RemaplyMapView]: the map view for classic Views and XML layouts.
- [io.github.winayaid.mapstrial.MapController]: changes the style, camera, and markers.
- [io.github.winayaid.mapstrial.MapListener]: receives map-ready, tap, camera, and error events.
- [io.github.winayaid.mapstrial.RemaplyMaps]: SDK version and logging.

```kotlin
val mapView = RemaplyMapView(context, MapStyle.url("https://example.com/style.json"))
mapView.listener = object : MapListener {
    override fun onMapReady(controller: MapController) {
        controller.addMarker(Marker("home", GeoCoordinate(48.8566, 2.3522)))
    }
}
```

The same API exists on iOS with identical names; see the API specification in the repository.

# Package io.github.winayaid.mapstrial

The public API of the Remaply Maps SDK: map view, controller, value types, events, errors, and logging.
