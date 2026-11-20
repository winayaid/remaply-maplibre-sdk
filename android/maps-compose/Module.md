# Module remaply-maps-trial-compose

Jetpack Compose support for the Remaply Maps SDK.

```kotlin
val controller = rememberMapController()
RemaplyMap(
    style = MapStyle.url("https://example.com/style.json"),
    modifier = Modifier.fillMaxSize(),
    controller = controller,
    onMapReady = { it.addMarker(Marker("home", GeoCoordinate(48.8566, 2.3522))) },
    onMarkerTap = { marker -> println("Tapped ${marker.id}") },
)
```

# Package com.remaply.maps.compose

[RemaplyMap] and [rememberMapController].
