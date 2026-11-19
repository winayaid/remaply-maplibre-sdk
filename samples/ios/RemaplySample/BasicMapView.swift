import RemaplyMaps
import SwiftUI

/// Sample 1: the smallest useful map. One style, one starting camera, and error reporting.
/// Everything else (gestures, lifecycle) is handled by the SDK.
struct BasicMapView: View {
    @State private var status = "Loading…"

    var body: some View {
        MapScreen {
            RemaplyMap(
                style: SampleScenario.style,
                options: MapOptions(initialCamera: CameraPosition(
                    center: GeoCoordinate(latitude: 48.8566, longitude: 2.3522), zoom: 11)),
                onMapReady: { _ in status = "Map ready. Pan, pinch, rotate, and tilt with your fingers." },
                onMapLoadError: { status = "Could not load the style: \($0.code)" }
            )
        } panel: {
            StatusText(text: status)
        }
        .navigationTitle("Basic map")
        .navigationBarTitleDisplayMode(.inline)
    }
}
