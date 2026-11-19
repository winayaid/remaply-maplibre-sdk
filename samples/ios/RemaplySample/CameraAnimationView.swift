import RemaplyMaps
import SwiftUI

/// Sample 3: moving the camera.
/// - `animateCamera` eases to a target; touching the map cancels it (completion receives false).
/// - `setCamera` jumps without animation.
/// - Camera events show the reason: PROGRAMMATIC for API calls, USER_GESTURE for gestures.
struct CameraAnimationView: View {
    @StateObject private var controller = MapController()
    @State private var status = "Loading map…"
    @State private var camera = ""

    private static let paris = GeoCoordinate(latitude: 48.8566, longitude: 2.3522)
    private static let tokyo = GeoCoordinate(latitude: 35.6812, longitude: 139.7671)
    private static let newYork = GeoCoordinate(latitude: 40.7128, longitude: -74.0060)

    var body: some View {
        MapScreen {
            RemaplyMap(
                style: SampleScenario.style,
                controller: controller,
                options: MapOptions(initialCamera: CameraPosition(center: Self.paris, zoom: 4)),
                onMapReady: { _ in status = "Pick a destination." },
                onCameraChange: { camera = SampleScenario.describe($0, reason: $1) },
                onCameraIdle: { camera = "Idle · \(SampleScenario.describe($0))" }
            )
        } panel: {
            StatusText(text: status)
            StatusText(text: camera, secondary: true)
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    SampleButton("Paris") { fly("Paris", CameraUpdate(center: Self.paris, zoom: 11, bearing: 0, tilt: 0)) }
                    SampleButton("Tokyo") { fly("Tokyo", CameraUpdate(center: Self.tokyo, zoom: 11, bearing: 0, tilt: 0)) }
                    SampleButton("New York") { fly("New York", CameraUpdate(center: Self.newYork, zoom: 11)) }
                }
                HStack {
                    SampleButton("Rotate 90°") { rotate() }
                    SampleButton("Tilt 45°") { fly("a 45° tilt", CameraUpdate(tilt: 45), durationMs: 600) }
                    SampleButton("Jump to world") {
                        try? controller.setCamera(CameraUpdate(
                            center: GeoCoordinate(latitude: 20, longitude: 0), zoom: 1, bearing: 0, tilt: 0))
                        status = "Jumped (setCamera, no animation)"
                    }
                }
            }
            .padding(.top, 4)
        }
        .navigationTitle("Camera animation")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func fly(_ name: String, _ update: CameraUpdate, durationMs: Int = 1500) {
        status = "Animating to \(name)…"
        try? controller.animateCamera(update, durationMs: durationMs) { finished in
            status = finished ? "Arrived at \(name)" : "Animation to \(name) cancelled"
        }
    }

    private func rotate() {
        let bearing = (controller.camera?.bearing ?? 0) + 90
        status = "Rotating…"
        try? controller.animateCamera(CameraUpdate(bearing: bearing), durationMs: 800) { finished in
            status = finished ? "Rotated to \(Int(bearing) % 360)°" : "Rotation cancelled"
        }
    }
}
