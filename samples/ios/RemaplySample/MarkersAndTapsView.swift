import RemaplyMaps
import SwiftUI

/// Sample 2: markers and taps.
/// - Tap the map to add a marker where you tapped.
/// - Tap a marker to bring it to the front and center the map on it.
/// - "Clear" removes every marker.
struct MarkersAndTapsView: View {
    @StateObject private var controller = MapController()
    @State private var status = "Loading map…"
    @State private var nextId = 1
    @State private var topZ = 0

    var body: some View {
        MapScreen {
            RemaplyMap(
                style: SampleScenario.style,
                controller: controller,
                options: MapOptions(initialCamera: CameraPosition(
                    center: GeoCoordinate(latitude: 48.8566, longitude: 2.3522), zoom: 12)),
                onMapReady: { _ in status = "Tap the map to add a marker." },
                onMapTap: { coordinate in
                    let id = "m\(nextId)"
                    nextId += 1
                    try? controller.addMarker(Marker(id: id, coordinate: coordinate))
                    status = "Added \(id) at \(SampleScenario.describe(coordinate)) · \(controller.markers.count) markers"
                },
                onMarkerTap: { marker in
                    topZ += 1
                    var updated = marker
                    updated.zIndex = topZ // a higher zIndex draws it on top
                    try? controller.updateMarker(updated)
                    try? controller.animateCamera(CameraUpdate(center: marker.coordinate), durationMs: 400)
                    status = "Tapped \(marker.id) (now on top)"
                }
            )
        } panel: {
            StatusText(text: status)
            HStack {
                SampleButton("Clear") {
                    controller.removeAllMarkers()
                    status = "All markers removed. Tap the map to add one."
                }
            }
            .padding(.top, 4)
        }
        .navigationTitle("Markers and taps")
        .navigationBarTitleDisplayMode(.inline)
    }
}
