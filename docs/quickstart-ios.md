# Quick start: iOS

Show a map with a marker in a new or existing iOS app. Allow about 10 minutes.

**You need:** Xcode 16 or newer, and an app with a deployment target of iOS 15.5 or higher.
You also need a map style URL. Examples here use the free, keyless
[OpenFreeMap](https://openfreemap.org) "Liberty" style; any MapLibre style works.

## 1. Add the package

1. In Xcode, choose **File › Add Package Dependencies…**
2. Enter `https://github.com/winayaid/remaply-maplibre-sdk`.
3. Set the dependency rule:
   **Up to Next Major Version**, `0.0.1`.
4. Add the **RemaplyMaps** product to your app target.

Or, in a `Package.swift`:

```swift
.package(url: "https://github.com/winayaid/remaply-maplibre-sdk", from: "0.0.1"),
// target dependency:
.product(name: "RemaplyMaps", package: "remaply-maplibre-sdk"),
```

No `Info.plist` keys are needed. MapLibre comes with the package as a binary framework.

## 2. Show a map

### SwiftUI

```swift
import RemaplyMaps
import SwiftUI

struct ContentView: View {
    @StateObject private var controller = MapController()
    private let paris = GeoCoordinate(latitude: 48.8566, longitude: 2.3522)

    var body: some View {
        RemaplyMap(
            style: .url("https://tiles.openfreemap.org/styles/liberty"),
            controller: controller,
            options: MapOptions(initialCamera: CameraPosition(center: paris, zoom: 12)),
            onMapReady: { controller in
                if controller.getMarker(id: "paris") == nil {
                    try? controller.addMarker(Marker(id: "paris", coordinate: paris))
                }
            },
            onMarkerTap: { marker in print("Tapped \(marker.id)") }
        )
        .ignoresSafeArea()
    }
}
```

### UIKit

```swift
import RemaplyMaps
import UIKit

final class MapViewController: UIViewController, MapListener {
    private let mapView = RemaplyMapView(style: .url("https://tiles.openfreemap.org/styles/liberty"))

    override func viewDidLoad() {
        super.viewDidLoad()
        mapView.frame = view.bounds
        mapView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        mapView.listener = self // held weakly
        view.addSubview(mapView)
    }

    func onMapReady(controller: MapController) {
        try? controller.addMarker(Marker(id: "paris",
            coordinate: GeoCoordinate(latitude: 48.8566, longitude: 2.3522)))
    }

    func onMarkerTap(marker: Marker) {
        print("Tapped \(marker.id)")
    }
}
```

The view manages its own lifecycle (window attachment, foreground and background).

## 3. Run it

Run the app. You should see Paris with a red pin; tapping the pin calls `onMarkerTap`.

## Next steps

- [Usage guide](guide.md): camera, markers, events, errors, lifecycle, and logging.
- [Samples](../samples/ios): basic map, markers and taps, camera animation, SwiftUI and UIKit.
- [Attribution](attribution.md): what your app must show and include.
- API reference: in Xcode, **Product › Build Documentation**, or see [API reference](api-reference.md).
