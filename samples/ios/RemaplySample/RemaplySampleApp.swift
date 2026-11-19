import MapLibre
import RemaplyMaps
import SwiftUI

/// Phase 1 foundation sample: shows a map with the demo style and nothing else.
///
/// This temporarily wraps MapLibre's `MLNMapView` directly (reached through the SDK's
/// dependency) to prove that the build, the pinned MapLibre version, and rendering all work.
/// Phase 3 replaces it with the SDK's `RemaplyMap` SwiftUI view.
@main
struct RemaplySampleApp: App {
    init() {
        print("Remaply Maps \(RemaplyMaps.version) (MapLibre \(RemaplyMaps.mapLibreVersion))")
    }

    var body: some Scene {
        WindowGroup {
            FoundationMapView(styleURL: SampleConstants.demoStyleURL)
                .ignoresSafeArea()
        }
    }
}

enum SampleConstants {
    /// Free, keyless demo style (API_SPEC.md §16). The SDK never hardcodes a style.
    static let demoStyleURL = "https://tiles.openfreemap.org/styles/liberty"
}

/// Minimal SwiftUI wrapper around `MLNMapView`. Phase 1 only.
struct FoundationMapView: UIViewRepresentable {
    let styleURL: String

    func makeUIView(context: Context) -> MLNMapView {
        let mapView = MLNMapView(frame: .zero, styleURL: URL(string: styleURL))
        mapView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        return mapView
    }

    func updateUIView(_ uiView: MLNMapView, context: Context) {}
}
