import Foundation
import MapLibre
import RemaplyMaps

/// Drives the SDK through a fixed sequence of public API calls and prints a normalized event
/// trace (`PARITY <step>|<event>`). The Android sample runs the same script; the two traces must
/// be identical. Used by the Phase 4 parity check (docs/parity-checklist.md).
///
/// Start it with the launch argument `-parity YES`.
@MainActor
final class ParityScript {
    private let map: RemaplyMapView
    private var step = "load"
    private var pendingChanges = 0
    private var lastChange: (CameraPosition, CameraChangeReason)?
    /// Camera values are omitted for this many upcoming change/idle lines (timing-dependent).
    private var volatileLines = 0
    private static let stepDelay: TimeInterval = 1.5
    // SDK-internal identifiers, used only by this test probe.
    private static let markerLayerID = "remaply-markers-layer"
    private static let markerIDProperty = "remaply_id"
    private static let paris11 = CameraUpdate(
        center: GeoCoordinate(latitude: 48.8566, longitude: 2.3522), zoom: 11, bearing: 0, tilt: 0
    )
    private static let paris14 = CameraUpdate(
        center: GeoCoordinate(latitude: 48.8566, longitude: 2.3522), zoom: 14, bearing: 0, tilt: 0
    )

    private var controller: MapController { map.controller }

    init(map: RemaplyMapView) {
        self.map = map
    }

    static var isRequested: Bool { UserDefaults.standard.bool(forKey: "parity") }

    // MARK: Events from the map listener

    func onStyleLoaded(_ style: MapStyle) {
        trace("styleLoaded \(describe(style)) markers=\(controller.markers.count)")
    }

    func onMapReady() {
        trace("mapReady isMapReady=\(controller.isMapReady)")
        runSteps(from: 0)
    }

    func onMapLoadError(_ error: MapError) { trace("loadError \(Self.code(error.code))") }

    func onCameraChange(_ camera: CameraPosition, _ reason: CameraChangeReason) {
        pendingChanges += 1
        lastChange = (camera, reason)
    }

    func onCameraIdle(_ camera: CameraPosition) { trace("idle \(format(camera))") }

    // MARK: Steps

    private lazy var steps: [(String, () -> Void)] = [
        ("markers", { [weak self] in
            guard let self else { return }
            SampleScenario.markers.forEach { try? self.controller.addMarker($0) }
            trace("markers \(controller.markers.map(\.id).joined(separator: ","))")
        }),
        ("frame", { [weak self] in
            guard let self else { return }
            try? controller.setCamera(Self.paris11)
        }),
        ("rendered", { [weak self] in
            guard let self else { return }
            trace("rendered \(renderedMarkerIds())")
        }),
        ("errors", { [weak self] in
            guard let self else { return }
            let origin = GeoCoordinate(latitude: 0, longitude: 0)
            expectError("addMarker(dup)") { try self.controller.addMarker(Marker(id: "a", coordinate: origin)) }
            expectError("addMarker(emptyId)") { try self.controller.addMarker(Marker(id: "", coordinate: origin)) }
            expectError("addMarker(badCoord)") {
                try self.controller.addMarker(Marker(id: "x", coordinate: GeoCoordinate(latitude: 0, longitude: 181)))
            }
            expectError("updateMarker(unknown)") { try self.controller.updateMarker(Marker(id: "zzz", coordinate: origin)) }
            trace("removeMarker(unknown)=\(controller.removeMarker(id: "zzz"))")
            expectError("setCamera(badCenter)") {
                try self.controller.setCamera(CameraUpdate(center: GeoCoordinate(latitude: 95, longitude: 0)))
            }
            expectError("setCamera(nanZoom)") { try self.controller.setCamera(CameraUpdate(zoom: .nan)) }
            expectError("animateCamera(negative)") {
                try self.controller.animateCamera(CameraUpdate(zoom: 3), durationMs: -1)
            }
        }),
        ("jump", { [weak self] in
            guard let self else { return }
            try? controller.setCamera(Self.paris14)
        }),
        ("jumpSame", { [weak self] in
            guard let self else { return }
            try? controller.setCamera(Self.paris14)
        }),
        ("jumpClamped", { [weak self] in
            guard let self else { return }
            try? controller.setCamera(CameraUpdate(zoom: 30, bearing: -90, tilt: 80))
        }),
        ("optionsClamp", { [weak self] in
            guard let self else { return }
            map.options.maxZoom = 13
        }),
        ("optionsRestore", { [weak self] in
            guard let self else { return }
            map.options.maxZoom = 22
        }),
        ("animate", { [weak self] in
            guard let self else { return }
            try? controller.animateCamera(CameraUpdate(zoom: 12, bearing: 90, tilt: 30), durationMs: 400) {
                self.trace("completion \($0)")
            }
        }),
        ("cancel", { [weak self] in
            guard let self else { return }
            volatileLines = 2 // the cancelled animation's changes and idle depend on timing
            try? controller.animateCamera(CameraUpdate(zoom: 13), durationMs: 1000) { self.trace("completion \($0)") }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                try? self.controller.setCamera(CameraUpdate(zoom: 10))
            }
        }),
        ("emptyAnimate", { [weak self] in
            guard let self else { return }
            try? controller.animateCamera(CameraUpdate()) { self.trace("completion \($0)") }
        }),
        ("styleErrors", { [weak self] in
            guard let self else { return }
            controller.setStyle(.bundled("missing.json"))
            controller.setStyle(.url("not-a-url"))
        }),
        ("markerAfterStyleError", { [weak self] in
            guard let self else { return }
            // The rejected style requests above must not stop marker rendering (Phase 4 fix).
            try? controller.addMarker(Marker(id: "d", coordinate: GeoCoordinate(latitude: 48.8700, longitude: 2.3600)))
            try? controller.setCamera(Self.paris11)
        }),
        ("renderedAfterStyleError", { [weak self] in
            guard let self else { return }
            trace("rendered \(renderedMarkerIds())")
        }),
        ("styleReload", { [weak self] in
            guard let self else { return }
            controller.setStyle(.bundled("styles/demo-bundled.json"))
        }),
        ("renderedAfterReload", { [weak self] in
            guard let self else { return }
            trace("rendered \(renderedMarkerIds())")
        }),
        ("styleSame", { [weak self] in
            guard let self else { return }
            controller.setStyle(.bundled("styles/demo-bundled.json"))
        }),
        ("markerEdit", { [weak self] in
            guard let self else { return }
            try? controller.updateMarker(
                Marker(id: "a", coordinate: GeoCoordinate(latitude: 48.8584, longitude: 2.2945), zIndex: 5)
            )
            trace("getMarker(a).zIndex=\(controller.getMarker(id: "a")?.zIndex ?? -1) markers=\(controller.markers.count)")
            controller.removeAllMarkers()
            trace("markers=\(controller.markers.count)")
        }),
        ("state", { [weak self] in
            guard let self else { return }
            trace("isMapReady=\(controller.isMapReady) style=\(controller.style.map(describe) ?? "nil")")
            if let camera = controller.camera { trace("camera \(format(camera))") }
        }),
    ]

    private func runSteps(from index: Int) {
        guard index < steps.count else {
            trace("done")
            return
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + Self.stepDelay) {
            let (name, action) = self.steps[index]
            self.step = name
            self.trace("begin")
            action()
            self.runSteps(from: index + 1)
        }
    }

    // MARK: Tracing

    private func expectError(_ label: String, _ block: () throws -> Void) {
        do {
            try block()
            trace("\(label) no error")
        } catch let error as MapError {
            trace("\(label) error \(Self.code(error.code))")
        } catch {
            trace("\(label) unexpected \(error)")
        }
    }

    private func trace(_ event: String) {
        flushChanges()
        emit(event)
    }

    private func flushChanges() {
        guard let (camera, reason) = lastChange else { return }
        let reasonName = reason == .userGesture ? "USER_GESTURE" : "PROGRAMMATIC"
        emit("change\(pendingChanges > 1 ? "+" : "") \(format(camera)) \(reasonName)")
        pendingChanges = 0
        lastChange = nil
    }

    private func emit(_ event: String) {
        print("PARITY \(step)|\(event)")
    }

    private func format(_ camera: CameraPosition) -> String {
        if volatileLines > 0 {
            volatileLines -= 1
            return "(timing-dependent)"
        }
        return String(format: "z=%.2f b=%.1f t=%.1f lat=%.4f lng=%.4f",
                      camera.zoom, camera.bearing, camera.tilt, camera.center.latitude, camera.center.longitude)
    }

    /// Marker ids actually drawn on screen, via the experimental native escape hatch.
    private func renderedMarkerIds() -> String {
        var ids = "(map not ready)"
        controller.withNativeMap { mapView in
            let features = mapView.visibleFeatures(in: mapView.bounds, styleLayerIdentifiers: [Self.markerLayerID])
            ids = Set(features.compactMap { $0.attribute(forKey: Self.markerIDProperty) as? String })
                .sorted().joined(separator: ",")
        }
        return ids
    }

    private func describe(_ style: MapStyle) -> String {
        switch style {
        case .url(let url): return "url:\(url)"
        case .bundled(let path): return "bundled:\(path)"
        }
    }

    /// Spec §3.1 mapping back to Kotlin names, so both traces use the same spelling.
    static func code(_ code: MapErrorCode) -> String {
        String(describing: code).replacingOccurrences(of: "([a-z])([A-Z])", with: "$1_$2", options: .regularExpression)
            .uppercased()
    }
}
