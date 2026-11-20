import RemaplyMaps
import SwiftUI

@main
struct RemaplySampleApp: App {
    var body: some Scene {
        WindowGroup {
            SampleLauncher()
        }
    }
}

/// The samples, in the order of the docs. Each is a small, self-contained screen.
enum Sample: String, CaseIterable, Identifiable {
    case basic = "Basic map"
    case markers = "Markers and taps"
    case camera = "Camera animation"
    case swiftUI = "SwiftUI example"
    case uiKit = "UIKit example"

    var id: String { rawValue }

    var description: String {
        switch self {
        case .basic: return "A style and a starting camera. Nothing else."
        case .markers: return "Add markers by tapping; tap a marker to select it."
        case .camera: return "Animate, jump, rotate, tilt; see camera events."
        case .swiftUI: return "The reference scenario with RemaplyMap."
        case .uiKit: return "The reference scenario with RemaplyMapView."
        }
    }
}

/// Sample launcher.
///
/// Testing hooks (launch arguments):
/// - `-parity YES` opens the UIKit example running the parity script (docs/parity-checklist.md).
/// - `-uiTesting YES`, `-styleUrl <url>`, or `-styleAsset <path>` open the SwiftUI example directly.
struct SampleLauncher: View {
    @State private var showUIKit = ParityScript.isRequested
    @State private var showSwiftUIExample = SampleScenario.opensSwiftUIExampleDirectly

    var body: some View {
        if showSwiftUIExample {
            SwiftUISampleView()
        } else {
            NavigationView {
                List {
                    Section(footer: Text("SDK \(RemaplyMaps.version) · MapLibre \(RemaplyMaps.mapLibreVersion)")) {
                        ForEach(Sample.allCases) { sample in
                            if sample == .uiKit {
                                Button { showUIKit = true } label: { row(sample) }
                            } else {
                                NavigationLink(destination: destination(sample)) { row(sample) }
                            }
                        }
                    }
                }
                .navigationTitle("Remaply Maps")
            }
            .navigationViewStyle(.stack)
            .fullScreenCover(isPresented: $showUIKit) {
                UIKitSampleScreen()
            }
        }
    }

    private func row(_ sample: Sample) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(sample.rawValue).foregroundColor(SampleColors.accent)
            Text(sample.description).font(.footnote).foregroundColor(.secondary)
        }
        .padding(.vertical, 4)
    }

    @ViewBuilder
    private func destination(_ sample: Sample) -> some View {
        switch sample {
        case .basic: BasicMapView()
        case .markers: MarkersAndTapsView()
        case .camera: CameraAnimationView()
        case .swiftUI: SwiftUISampleView()
        case .uiKit: EmptyView()
        }
    }
}

/// Sample 4: the SwiftUI example. Runs the reference scenario (API_SPEC.md §16), which the
/// Android Compose example mirrors: three markers, taps, an animation, and every event.
struct SwiftUISampleView: View {
    @StateObject private var controller = MapController()
    @State private var status = "Loading map…"
    @State private var camera = ""
    private let style = SampleScenario.style

    var body: some View {
        MapScreen {
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
        } panel: {
            StatusText(text: status)
            StatusText(text: camera, secondary: true)
            HStack {
                SampleButton("Animate") {
                    SampleScenario.animate(controller) { finished in status = "Animation finished: \(finished)" }
                }
                SampleButton("Reset") { SampleScenario.reset(controller) }
            }
            .padding(.top, 4)
        }
        .navigationTitle("SwiftUI example")
        .navigationBarTitleDisplayMode(.inline)
    }
}
