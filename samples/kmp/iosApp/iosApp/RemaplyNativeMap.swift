import RemaplyMaps
@preconcurrency import Shared
import UIKit

// The bridge between the shared Kotlin code and the Remaply iOS SDK. Kotlin/Native can't call the SDK's
// Swift API, so Kotlin declares the small NativeMap interfaces (shared/src/iosMain/.../NativeMap.kt) and this
// file implements them with RemaplyMapView. Compose calls them on the main thread, hence @MainActor.

@MainActor
final class RemaplyNativeMapFactory: NSObject, NativeMapFactory {
    func createMap(
        styleUrl: String,
        initialCamera: CameraTarget,
        places: [Place],
        listener: NativeMapListener
    ) -> NativeMap {
        RemaplyNativeMap(styleUrl: styleUrl, initialCamera: initialCamera, places: places, listener: listener)
    }
}

@MainActor
final class RemaplyNativeMap: NSObject, NativeMap, MapListener {
    private let mapView: RemaplyMapView
    private let places: [Place]
    private let listener: NativeMapListener

    var view: UIView { mapView }

    init(styleUrl: String, initialCamera: CameraTarget, places: [Place], listener: NativeMapListener) {
        mapView = RemaplyMapView(
            style: .url(styleUrl),
            options: MapOptions(initialCamera: CameraPosition(center: initialCamera.coordinate, zoom: initialCamera.zoom))
        )
        self.places = places
        self.listener = listener
        super.init()
        mapView.listener = self // held weakly; Compose keeps this object alive
    }

    func flyTo(target: CameraTarget) {
        try? mapView.controller.animateCamera(
            CameraUpdate(center: target.coordinate, zoom: target.zoom),
            durationMs: 1500
        )
    }

    // MARK: MapListener

    func onMapReady(controller: MapController) {
        for place in places where controller.getMarker(id: place.id) == nil {
            try? controller.addMarker(Marker(
                id: place.id,
                coordinate: GeoCoordinate(latitude: place.latitude, longitude: place.longitude)
            ))
        }
        listener.onMapReady()
    }

    func onMarkerTap(marker: Marker) {
        listener.onPlaceTap(placeId: marker.id)
    }

    func onMapTap(coordinate: GeoCoordinate) {
        listener.onMapTap(latitude: coordinate.latitude, longitude: coordinate.longitude)
    }
}

private extension CameraTarget {
    var coordinate: GeoCoordinate { GeoCoordinate(latitude: latitude, longitude: longitude) }
}
