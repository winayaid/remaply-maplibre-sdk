import RemaplyMaps
import SwiftUI

@main
struct RemaplySampleApp: App {
    init() {
        print("Remaply Maps \(RemaplyMaps.version) (MapLibre \(RemaplyMaps.mapLibreVersion))")
    }

    var body: some Scene {
        WindowGroup {
            SwiftUISampleView()
        }
    }
}

/// SwiftUI sample: the reference scenario with `RemaplyMap`.
struct SwiftUISampleView: View {
    @StateObject private var controller = MapController()
    @State private var status = "Loading map…"
    @State private var camera = ""
    @State private var showUIKitSample = ParityScript.isRequested
    private let style = SampleScenario.style

    var body: some View {
        ZStack(alignment: .top) {
            RemaplyMap(
                style: style,
                controller: controller,
                options: SampleScenario.options,
                onMapReady: { controller in
                    SampleScenario.addMarkersIfNeeded(controller)
                    status = "Map ready · \(controller.markers.count) markers"
                },
                onMapLoadError: { status = "Load error: \($0.code)" },
                onMapTap: { status = "Map tap: \(SampleScenario.describe($0))" },
                onMarkerTap: { status = "Marker tap: \($0.id)" },
                onCameraChange: { camera = SampleScenario.describe($0, reason: $1) },
                onCameraIdle: { camera = "Idle · \(SampleScenario.describe($0))" }
            )
            .ignoresSafeArea()

            VStack(alignment: .leading, spacing: 4) {
                Text(status).font(.system(size: 15)).accessibilityIdentifier("status")
                Text(camera).font(.system(size: 13)).foregroundColor(.secondary).accessibilityIdentifier("camera")
                HStack {
                    SampleButton("Animate") {
                        SampleScenario.animate(controller) { finished in status = "Animation finished: \(finished)" }
                    }
                    SampleButton("Reset") { SampleScenario.reset(controller) }
                    SampleButton("UIKit sample") { showUIKitSample = true }
                }
                .padding(.top, 4)
            }
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.white.opacity(0.9))
            .cornerRadius(8)
            .padding(12)
        }
        .fullScreenCover(isPresented: $showUIKitSample) {
            UIKitSampleScreen()
        }
    }
}

struct SampleButton: View {
    private let title: String
    private let action: () -> Void

    init(_ title: String, action: @escaping () -> Void) {
        self.title = title
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 14))
                .foregroundColor(.white)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(Color(red: 0, green: 0x79 / 255.0, blue: 0x6B / 255.0))
                .cornerRadius(6)
        }
    }
}
