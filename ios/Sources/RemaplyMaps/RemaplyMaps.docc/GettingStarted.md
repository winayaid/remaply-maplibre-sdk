# Getting started

Add the package, show a map, add a marker, and react to taps.

## Add the package

In Xcode, choose **File › Add Package Dependencies…**, enter
`https://github.com/winayaid/remaply-maplibre-sdk`, and add the `RemaplyMaps` product to your app
target. The minimum deployment target is iOS 15.5.

## Show a map in SwiftUI

Keep a ``MapController`` in a `@StateObject` so it survives view updates, and pass it to
``RemaplyMap``:

```swift
struct MapScreen: View {
    @StateObject private var controller = MapController()

    var body: some View {
        RemaplyMap(
            style: .url("https://example.com/style.json"),
            controller: controller,
            options: MapOptions(initialCamera: CameraPosition(
                center: GeoCoordinate(latitude: 48.8566, longitude: 2.3522), zoom: 12)),
            onMapReady: { controller in
                try? controller.addMarker(Marker(id: "home",
                    coordinate: GeoCoordinate(latitude: 48.8566, longitude: 2.3522)))
            },
            onMarkerTap: { marker in print("Tapped \(marker.id)") }
        )
        .ignoresSafeArea()
    }
}
```

## Show a map in UIKit

``RemaplyMapView`` handles its lifecycle by itself. Set a ``MapListener`` (held weakly):

```swift
final class MapViewController: UIViewController, MapListener {
    private let mapView = RemaplyMapView(style: .url("https://example.com/style.json"))

    override func viewDidLoad() {
        super.viewDidLoad()
        mapView.frame = view.bounds
        mapView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        mapView.listener = self
        view.addSubview(mapView)
    }

    func onMapReady(controller: MapController) {
        try? controller.addMarker(Marker(id: "home",
            coordinate: GeoCoordinate(latitude: 48.8566, longitude: 2.3522)))
    }

    func onMarkerTap(marker: Marker) {
        print("Tapped \(marker.id)")
    }
}
```

## Move the camera

```swift
try controller.animateCamera(CameraUpdate(zoom: 14, bearing: 45), durationMs: 800) { finished in
    print("Animation finished: \(finished)")
}
```

Calls that can reject their input throw ``MapError``; style problems arrive in
``MapListener/onMapLoadError(error:)``.

## Attribution

Keep the map's attribution button visible, and include MapLibre's license notice in your app's
acknowledgements. See `docs/attribution.md` in the repository.
