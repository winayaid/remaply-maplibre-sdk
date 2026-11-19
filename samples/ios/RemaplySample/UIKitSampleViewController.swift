import RemaplyMaps
import SwiftUI
import UIKit

/// UIKit sample: the reference scenario with `RemaplyMapView`.
/// Lifecycle is automatic; no forwarding of view controller callbacks is needed.
final class UIKitSampleViewController: UIViewController, MapListener {
    private let mapView = RemaplyMapView(style: SampleScenario.style, options: SampleScenario.options)
    private let statusLabel = UILabel()
    var onClose: (() -> Void)?

    override func viewDidLoad() {
        super.viewDidLoad()
        mapView.frame = view.bounds
        mapView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        mapView.listener = self
        view.addSubview(mapView)

        statusLabel.text = "Loading map…"
        statusLabel.font = .systemFont(ofSize: 15)
        statusLabel.accessibilityIdentifier = "status"

        let buttons = UIStackView(arrangedSubviews: [
            makeButton("Animate") { [weak self] in
                guard let self else { return }
                SampleScenario.animate(self.mapView.controller) { finished in
                    self.statusLabel.text = "Animation finished: \(finished)"
                }
            },
            makeButton("Reset") { [weak self] in
                guard let self else { return }
                SampleScenario.reset(self.mapView.controller)
            },
            makeButton("Close") { [weak self] in self?.onClose?() },
        ])
        buttons.spacing = 8

        let panel = UIStackView(arrangedSubviews: [statusLabel, buttons])
        panel.axis = .vertical
        panel.alignment = .leading
        panel.spacing = 8
        panel.isLayoutMarginsRelativeArrangement = true
        panel.directionalLayoutMargins = NSDirectionalEdgeInsets(top: 12, leading: 12, bottom: 12, trailing: 12)
        panel.backgroundColor = UIColor.white.withAlphaComponent(0.9)
        panel.layer.cornerRadius = 8
        panel.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(panel)
        NSLayoutConstraint.activate([
            panel.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 12),
            panel.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor, constant: 12),
            panel.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor, constant: -12),
        ])
    }

    private func makeButton(_ title: String, action: @escaping () -> Void) -> UIButton {
        var config = UIButton.Configuration.filled()
        config.title = title
        config.baseBackgroundColor = UIColor(red: 0, green: 0x79 / 255.0, blue: 0x6B / 255.0, alpha: 1)
        return UIButton(configuration: config, primaryAction: UIAction { _ in action() })
    }

    // MARK: MapListener

    func onMapReady(controller: MapController) {
        SampleScenario.addMarkersIfNeeded(controller)
        statusLabel.text = "Map ready · \(controller.markers.count) markers"
    }

    func onMapLoadError(error: MapError) {
        statusLabel.text = "Load error: \(error.code)"
    }

    func onMapTap(coordinate: GeoCoordinate) {
        statusLabel.text = "Map tap: \(SampleScenario.describe(coordinate))"
    }

    func onMarkerTap(marker: Marker) {
        statusLabel.text = "Marker tap: \(marker.id)"
    }

    func onCameraIdle(camera: CameraPosition) {
        statusLabel.text = SampleScenario.describe(camera)
    }
}

/// Presents the UIKit sample from SwiftUI.
struct UIKitSampleScreen: UIViewControllerRepresentable {
    @Environment(\.dismiss) private var dismiss

    func makeUIViewController(context: Context) -> UIKitSampleViewController {
        let controller = UIKitSampleViewController()
        controller.onClose = { dismiss() }
        return controller
    }

    func updateUIViewController(_ controller: UIKitSampleViewController, context: Context) {}
}
