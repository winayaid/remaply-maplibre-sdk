import Foundation
import RemaplyMaps

/// The reference parity scenario (API_SPEC.md §16), shared by the SwiftUI and UIKit samples
/// and mirrored by the Android sample.
enum SampleScenario {
    /// Free, keyless demo style. The SDK never hardcodes a style.
    static let defaultStyle: MapStyle = .url("https://tiles.openfreemap.org/styles/liberty")

    /// Manual-testing hook, matching Android's intent extras:
    /// launch with `-styleUrl <url>` or `-styleAsset <path>` to load a different style.
    static var style: MapStyle {
        let defaults = UserDefaults.standard
        if let url = defaults.string(forKey: "styleUrl") { return .url(url) }
        if let asset = defaults.string(forKey: "styleAsset") { return .bundled(asset) }
        return defaultStyle
    }

    /// UI tests launch with `-uiTesting YES`: the camera starts centered on marker "b" at zoom 14,
    /// so tap targets do not depend on the screen size.
    static var isUITesting: Bool { UserDefaults.standard.bool(forKey: "uiTesting") }

    static var options: MapOptions {
        if isUITesting {
            return MapOptions(initialCamera: CameraPosition(center: markers[1].coordinate, zoom: 14))
        }
        return MapOptions(
            initialCamera: CameraPosition(center: GeoCoordinate(latitude: 48.8566, longitude: 2.3522), zoom: 12)
        )
    }

    static let markers: [Marker] = [
        Marker(id: "a", coordinate: GeoCoordinate(latitude: 48.8584, longitude: 2.2945)),
        Marker(id: "b", coordinate: GeoCoordinate(latitude: 48.8606, longitude: 2.3376), zIndex: 1),
        Marker(id: "c", coordinate: GeoCoordinate(latitude: 48.8530, longitude: 2.3499)),
    ]

    /// Adds the markers once. A controller that already has them (for example, kept across
    /// a view rebuild) is left alone.
    @MainActor
    static func addMarkersIfNeeded(_ controller: MapController) {
        guard controller.markers.isEmpty else { return }
        for marker in markers {
            try? controller.addMarker(marker)
        }
    }

    @MainActor
    static func animate(_ controller: MapController, onDone: @escaping (Bool) -> Void) {
        try? controller.animateCamera(CameraUpdate(zoom: 13, bearing: 45), durationMs: 1000, completion: onDone)
    }

    @MainActor
    static func reset(_ controller: MapController) {
        try? controller.setCamera(
            CameraUpdate(center: GeoCoordinate(latitude: 48.8566, longitude: 2.3522), zoom: 12, bearing: 0, tilt: 0)
        )
    }

    static func describe(_ coordinate: GeoCoordinate) -> String {
        String(format: "%.5f, %.5f", coordinate.latitude, coordinate.longitude)
    }

    static func describe(_ camera: CameraPosition, reason: CameraChangeReason? = nil) -> String {
        let base = String(format: "zoom %.2f · bearing %.0f° · tilt %.0f°", camera.zoom, camera.bearing, camera.tilt)
        guard let reason else { return base }
        return "\(base) (\(reason == .userGesture ? "USER_GESTURE" : "PROGRAMMATIC"))"
    }
}
