import MapLibre
import UIKit

/// Receives view-level events. Only called while the session is bound and alive.
@MainActor
protocol MapSessionEventSink: AnyObject {
    func sessionMapReady()
    func sessionMapLoadError(_ error: MapError)
    func sessionStyleLoaded(_ style: MapStyle)
    func sessionMapTap(_ coordinate: GeoCoordinate)
    func sessionMarkerTap(_ marker: Marker)
    func sessionCameraChange(_ camera: CameraPosition, reason: CameraChangeReason)
    func sessionCameraIdle(_ camera: CameraPosition)
}

/// One native map inside one view: owns the `MLNMapView` and implements everything the controller
/// asks of a view (``MapBinding``) and the lifecycle steps (``LifecycleTarget``).
///
/// Camera events: programmatic moves (setCamera, animations, zoom-limit clamps) are reported by
/// this class directly, so their events are deterministic and identical to Android. Native
/// MapLibre camera callbacks are only used for user gestures.
@MainActor
final class MapSession: NSObject, MapBinding, LifecycleTarget, MLNMapViewDelegate, UIGestureRecognizerDelegate {
    private enum StyleState { case none, loading, loaded, failed }

    struct RunningAnimation {
        let animation: CameraAnimation
        let completion: ((Bool) -> Void)?
        var startTime: CFTimeInterval?
    }

    /// Empty style shown until the requested one loads, so MapLibre never fetches its default style.
    private static let placeholderStyleJSON = #"{"version":8,"sources":{},"layers":[]}"#
    private static let controlMargin: CGFloat = 8
    static let tapRadius: CGFloat = 8

    private weak var container: UIView?
    private weak var sink: MapSessionEventSink?
    let controller: MapController
    private var options: MapOptions

    private(set) var mapView: MLNMapView?
    private let markerLayer = MarkerLayer()
    var tapRecognizer: UITapGestureRecognizer?

    private var ready = false
    private var destroyed = false
    private var unbound = false

    private var styleState = StyleState.none
    private var activeStyle: MapStyle?
    private var expectedStyleURL: URL?

    private var markerSyncScheduled = false
    var running: RunningAnimation?
    var frameDriver: FrameDriver?
    /// > 0 while this class moves the camera itself; native callbacks are ignored then.
    var programmaticDepth = 0

    lazy var gate = CameraEventGate(
        emitChange: { [weak self] camera, reason in
            self?.postEvent { $0.sessionCameraChange(camera, reason: reason) }
        },
        emitIdle: { [weak self] camera in
            guard let self else { return }
            // Keep the controller's stored camera current, so it survives the view going away.
            if self.controller.binding === self { self.controller.storedCamera = camera }
            self.postEvent { $0.sessionCameraIdle(camera) }
        }
    )

    init(container: UIView, controller: MapController, options: MapOptions, sink: MapSessionEventSink) {
        self.container = container
        self.controller = controller
        self.options = options
        self.sink = sink
        super.init()
        self.options = sanitize(options)
    }

    // MARK: - MapBinding

    var isReady: Bool { ready && !destroyed && !unbound }
    var minZoom: Double { options.minZoom }
    var maxZoom: Double { options.maxZoom }
    var nativeMapView: MLNMapView? { mapView }

    func currentCamera() -> CameraPosition {
        guard let mapView else { return controller.storedCamera ?? CameraMath.defaultCamera }
        let center = mapView.centerCoordinate
        return CameraMath.normalize(
            CameraPosition(
                center: GeoCoordinate(latitude: center.latitude, longitude: center.longitude),
                zoom: mapView.zoomLevel,
                bearing: mapView.direction,
                tilt: Double(mapView.camera.pitch)
            ),
            minZoom: -.infinity,
            maxZoom: .infinity
        )
    }

    func requestStyle(_ style: MapStyle) {
        guard !destroyed, !unbound else { return }
        if style == activeStyle, styleState == .loading || styleState == .loaded { return }
        loadStyle(style)
    }

    func jump(to target: CameraPosition) {
        guard isReady else { return }
        cancelAnimation()
        moveNative(target)
        gate.idle(currentCamera(), reason: .programmatic)
    }

    func animate(to target: CameraPosition, durationMs: Int, completion: ((Bool) -> Void)?) {
        guard isReady else {
            if let completion { controller.events.post { completion(false) } }
            return
        }
        cancelAnimation()
        running = RunningAnimation(
            animation: CameraAnimation(from: currentCamera(), to: target, durationMs: durationMs),
            completion: completion
        )
        let driver = FrameDriver { [weak self] timestamp in self?.onAnimationFrame(timestamp) }
        frameDriver = driver
        driver.start()
    }

    func cancelAnimation() {
        guard let run = running else { return }
        running = nil
        frameDriver?.stop()
        frameDriver = nil
        // Close the movement sequence, then report the cancellation (API_SPEC.md §6.4, §8.3).
        gate.idle(currentCamera(), reason: .programmatic)
        if let completion = run.completion { controller.events.post { completion(false) } }
    }

    func markersChanged() {
        guard isReady, !markerSyncScheduled else { return }
        markerSyncScheduled = true
        DispatchQueue.main.async { [weak self] in
            guard let self else { return }
            self.markerSyncScheduled = false
            if self.isReady { self.syncMarkers() }
        }
    }

    func unboundByController() {
        cancelAnimation()
        unbound = true
        gate.disable()
        if let style = mapView?.style, styleState == .loaded {
            markerLayer.sync([], in: style)
        }
    }

    // MARK: - Lifecycle

    func performCreate() {
        MapLibreLogBridge.install()
        guard let container else { return }
        let view = MLNMapView(frame: container.bounds, styleJSON: Self.placeholderStyleJSON)
        view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.delegate = self
        container.insertSubview(view, at: 0)
        mapView = view

        configureControls(view)
        view.maximumPitch = CGFloat(CameraMath.maxTilt)
        applyOptions(to: view)
        installTapRecognizer(on: view)

        if controller.storedCamera == nil, let initial = options.initialCamera {
            controller.storedCamera = CameraMath.normalize(initial, minZoom: options.minZoom, maxZoom: options.maxZoom)
        }
        controller.bind(self)
        if let camera = controller.storedCamera { moveNative(camera) }
        if let style = controller.style { loadStyle(style) }
    }

    // MLNMapView pauses and resumes rendering on its own when the app moves between foreground
    // and background, so start/resume/pause/stop only track state on iOS.
    func performStart() {}
    func performResume() {}
    func performPause() {}
    func performStop() {}

    func performDestroy() {
        cancelAnimation()
        controller.unbind(self)
        destroyed = true
        ready = false
        gate.disable()
        if let tapRecognizer { mapView?.removeGestureRecognizer(tapRecognizer) }
        tapRecognizer = nil
        mapView?.delegate = nil
        mapView?.removeFromSuperview()
        markerLayer.reset()
        mapView = nil
    }

    func onLowMemory() {
        // MLNMapView releases its caches on memory warnings by itself.
        SdkLog.debug("Low memory warning received")
    }

    // MARK: - Options

    func applyOptions(_ newOptions: MapOptions) {
        options = sanitize(newOptions)
        if let mapView { applyOptions(to: mapView) }
    }

    private func applyOptions(to view: MLNMapView) {
        view.isScrollEnabled = options.scrollGesturesEnabled
        view.isZoomEnabled = options.zoomGesturesEnabled
        view.isRotateEnabled = options.rotateGesturesEnabled
        view.isPitchEnabled = options.tiltGesturesEnabled
        view.showsCompassView = options.compassEnabled
        programmatic {
            view.minimumZoomLevel = options.minZoom
            view.maximumZoomLevel = options.maxZoom
            let camera = currentCamera()
            if camera.zoom < options.minZoom || camera.zoom > options.maxZoom {
                var clamped = camera
                clamped.zoom = min(max(camera.zoom, options.minZoom), options.maxZoom)
                moveNative(clamped)
            }
        }
        // If the new limits moved the camera, report it as a programmatic change (API_SPEC.md §7.5).
        if isReady && running == nil { gate.idle(currentCamera(), reason: .programmatic) }
    }

    private func sanitize(_ options: MapOptions) -> MapOptions {
        let (fixed, warnings) = CameraMath.sanitize(options)
        warnings.forEach { SdkLog.warn("MapOptions: \($0)") }
        return fixed
    }

    private func configureControls(_ view: MLNMapView) {
        let margin = CGPoint(x: Self.controlMargin, y: Self.controlMargin)
        view.compassViewPosition = .topRight
        view.compassViewMargins = margin
        view.showsLogoView = true
        view.logoViewPosition = .bottomLeft
        view.logoViewMargins = margin
        view.showsAttributionButton = true
        view.attributionButtonPosition = .bottomRight
        view.attributionButtonMargins = margin
        view.showsScale = false
    }

    // MARK: - Style loading (API_SPEC.md §6.2, §8.2)

    private func loadStyle(_ style: MapStyle) {
        guard let mapView else { return }
        activeStyle = style
        styleState = .loading
        expectedStyleURL = nil
        if let (code, message) = StyleValidation.problem(style) {
            failStyle(makeMapError(code, message))
            return
        }
        let url: URL
        switch style {
        case .url(let string):
            guard let parsed = URL(string: string.trimmingCharacters(in: .whitespaces)) else {
                failStyle(makeMapError(.invalidArgument, "Style URL is not a valid URL: '\(string)'"))
                return
            }
            url = parsed
        case .bundled(let path):
            guard let fileURL = Self.bundledStyleURL(path) else {
                failStyle(makeMapError(.styleNotFound, "Bundled style not found in the app bundle: '\(path)'"))
                return
            }
            url = fileURL
        }
        markerLayer.reset()
        expectedStyleURL = url
        mapView.styleURL = url
    }

    private static func bundledStyleURL(_ path: String) -> URL? {
        guard let base = Bundle.main.resourceURL else { return nil }
        let url = base.appendingPathComponent(path)
        var isDirectory: ObjCBool = false
        guard FileManager.default.fileExists(atPath: url.path, isDirectory: &isDirectory), !isDirectory.boolValue else {
            return nil
        }
        return url
    }

    private func failStyle(_ error: MapError) {
        styleState = .failed
        expectedStyleURL = nil
        postEvent { $0.sessionMapLoadError(error) }
    }

    nonisolated func mapView(_ mapView: MLNMapView, didFinishLoading style: MLNStyle) {
        MainActor.assumeIsolated { onStyleFinishedLoading(style) }
    }

    nonisolated func mapViewDidFailLoadingMap(_ mapView: MLNMapView, withError error: Error) {
        MainActor.assumeIsolated { onLoadFailure(error) }
    }

    nonisolated func mapViewRendererDidError(_ mapView: MLNMapView) {
        MainActor.assumeIsolated {
            let error = makeMapError(.rendererInitFailed, "The MapLibre renderer reported an error")
            postEvent { $0.sessionMapLoadError(error) }
        }
    }

    private func onStyleFinishedLoading(_ style: MLNStyle) {
        guard !destroyed, !unbound, styleState == .loading,
              let expected = expectedStyleURL, mapView?.styleURL == expected,
              let requested = activeStyle
        else { return } // e.g. the placeholder style, or a superseded request
        styleState = .loaded
        markerLayer.install(in: style)
        markerLayer.sync(controller.markersInDrawOrder(), in: style)
        let first = !ready
        if first {
            if let camera = controller.storedCamera { moveNative(camera) }
            ready = true
            gate.enable(current: currentCamera())
        }
        postEvent { $0.sessionStyleLoaded(requested) }
        if first { postEvent { $0.sessionMapReady() } }
    }

    private func onLoadFailure(_ error: Error) {
        if styleState == .loading, expectedStyleURL != nil {
            failStyle(makeMapError(.styleLoadFailed, "Style failed to load: \(error.localizedDescription)", cause: error))
        } else {
            // Tiles, sprites, glyphs: not a style failure (API_SPEC.md §8.2).
            SdkLog.warn("Map resource failed to load: \(error.localizedDescription)", error: error)
        }
    }

    private func syncMarkers() {
        guard let style = mapView?.style, styleState == .loaded else { return }
        markerLayer.sync(controller.markersInDrawOrder(), in: style)
    }

    // MARK: - Camera

    func moveNative(_ target: CameraPosition) {
        guard let mapView else { return }
        programmatic {
            if Double(mapView.camera.pitch) != target.tilt {
                let camera = mapView.camera
                camera.pitch = CGFloat(target.tilt)
                mapView.setCamera(camera, animated: false)
            }
            // Sets zoom exactly; MLNMapCamera itself is altitude-based.
            mapView.setCenter(
                CLLocationCoordinate2D(latitude: target.center.latitude, longitude: target.center.longitude),
                zoomLevel: target.zoom,
                direction: target.bearing,
                animated: false
            )
        }
    }

    func programmatic(_ block: () -> Void) {
        programmaticDepth += 1
        defer { programmaticDepth -= 1 }
        block()
    }

    // MARK: - Events

    /// Delivers a view event, unless this session was destroyed or unbound in the meantime.
    func postEvent(_ event: @escaping (MapSessionEventSink) -> Void) {
        controller.events.post { [weak self] in
            guard let self, !self.destroyed, !self.unbound, let sink = self.sink else { return }
            event(sink)
        }
    }
}
