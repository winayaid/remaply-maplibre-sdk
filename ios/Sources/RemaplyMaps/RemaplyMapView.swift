import MapLibre
import UIKit

/// A map view for UIKit.
///
/// ```swift
/// let mapView = RemaplyMapView(frame: view.bounds, style: .url("https://example.com/style.json"))
/// mapView.listener = self   // held weakly
/// ```
///
/// **Lifecycle:** handled automatically (window attachment, app foreground/background, memory
/// warnings). The lifecycle methods exist for parity with Android and for releasing the map early;
/// they are idempotent. A view that is deallocated without ``onDestroy()`` still releases its map,
/// and its controller keeps the last camera, style, and markers.
///
/// Spec: API-033.
@MainActor
public final class RemaplyMapView: UIView {
    /// The controller for this view's style, camera, and markers.
    public let controller: MapController

    /// Map options. Setting this applies every field except ``MapOptions/initialCamera`` immediately.
    public var options: MapOptions {
        didSet {
            guard lifecycleState != .destroyed else {
                SdkLog.warn("MAP_DESTROYED: options set after onDestroy")
                return
            }
            session?.applyOptions(options)
        }
    }

    /// Receives map events. Held weakly (the usual delegate pattern).
    public weak var listener: MapListener?

    /// Current lifecycle state. Spec: API-010.
    public var lifecycleState: MapLifecycleState { stateMachine.state }

    private var session: MapSession?
    private lazy var stateMachine = LifecycleStateMachine(target: self)

    /// Creates a map view.
    ///
    /// - Parameters:
    ///   - frame: Initial frame.
    ///   - style: Initial style. `nil` keeps the controller's stored style, if any.
    ///   - options: Map options.
    ///   - controller: The controller to bind. `nil` creates a new one.
    public init(
        frame: CGRect = .zero,
        style: MapStyle? = nil,
        options: MapOptions = MapOptions(),
        controller: MapController? = nil
    ) {
        self.controller = controller ?? MapController()
        self.options = options
        super.init(frame: frame)
        if let style, self.controller.style != style { self.controller.setStyle(style) }
        commonInit()
    }

    /// Storyboard / XIB initializer. Starts with no style: call `controller.setStyle(_:)`.
    public required init?(coder: NSCoder) {
        controller = MapController()
        options = MapOptions()
        super.init(coder: coder)
        commonInit()
    }

    private func commonInit() {
        clipsToBounds = true
        let center = NotificationCenter.default
        center.addObserver(self, selector: #selector(appDidEnterBackground),
                           name: UIApplication.didEnterBackgroundNotification, object: nil)
        center.addObserver(self, selector: #selector(appWillEnterForeground),
                           name: UIApplication.willEnterForegroundNotification, object: nil)
        center.addObserver(self, selector: #selector(appDidBecomeActive),
                           name: UIApplication.didBecomeActiveNotification, object: nil)
        center.addObserver(self, selector: #selector(appWillResignActive),
                           name: UIApplication.willResignActiveNotification, object: nil)
        center.addObserver(self, selector: #selector(appDidReceiveMemoryWarning),
                           name: UIApplication.didReceiveMemoryWarningNotification, object: nil)
    }

    // MARK: - Manual lifecycle (API_SPEC.md §9.1)

    /// Creates the native map.
    public func onCreate() { stateMachine.moveUp(to: .created) }

    /// Starts rendering.
    public func onStart() { stateMachine.moveUp(to: .started) }

    /// Resumes interaction.
    public func onResume() { stateMachine.moveUp(to: .resumed) }

    /// Pauses interaction.
    public func onPause() { stateMachine.moveDown(to: .started) }

    /// Stops rendering.
    public func onStop() { stateMachine.moveDown(to: .created) }

    /// Releases the native map. Cancels animations, unbinds the controller (which keeps its state),
    /// and clears the listener. Terminal.
    public func onDestroy() {
        stateMachine.destroy()
        listener = nil
    }

    /// Releases map caches.
    public func onLowMemory() {
        session?.onLowMemory()
    }

    // MARK: - Automatic lifecycle

    override public func didMoveToWindow() {
        super.didMoveToWindow()
        guard lifecycleState != .destroyed else { return }
        if window != nil {
            stateMachine.moveUp(to: Self.isAppActive ? .resumed : .started)
        } else {
            stateMachine.moveDown(to: .created)
        }
    }

    @objc private func appDidEnterBackground() {
        guard lifecycleState != .destroyed else { return }
        stateMachine.moveDown(to: .created)
    }

    @objc private func appWillEnterForeground() {
        guard lifecycleState != .destroyed, window != nil else { return }
        stateMachine.moveUp(to: .started)
    }

    @objc private func appDidBecomeActive() {
        guard lifecycleState != .destroyed, window != nil else { return }
        stateMachine.moveUp(to: .resumed)
    }

    @objc private func appWillResignActive() {
        guard lifecycleState != .destroyed else { return }
        stateMachine.moveDown(to: .started)
    }

    @objc private func appDidReceiveMemoryWarning() {
        onLowMemory()
    }

    private static var isAppActive: Bool {
        UIApplication.shared.applicationState == .active
    }

    // MARK: - Touch: any touch cancels a running camera animation (API_SPEC.md §6.4)

    override public func hitTest(_ point: CGPoint, with event: UIEvent?) -> UIView? {
        let hit = super.hitTest(point, with: event)
        if hit != nil, event?.type == .touches { session?.cancelAnimation() }
        return hit
    }
}

// MARK: - Lifecycle steps

extension RemaplyMapView: LifecycleTarget {
    func performCreate() {
        let newSession = MapSession(container: self, controller: controller, options: options, sink: self)
        session = newSession
        newSession.performCreate()
    }

    func performStart() { session?.performStart() }
    func performResume() { session?.performResume() }
    func performPause() { session?.performPause() }
    func performStop() { session?.performStop() }

    func performDestroy() {
        session?.performDestroy()
        session = nil
    }
}

// MARK: - Event delivery

extension RemaplyMapView: MapSessionEventSink {
    func sessionMapReady() { listener?.onMapReady(controller: controller) }
    func sessionMapLoadError(_ error: MapError) { listener?.onMapLoadError(error: error) }
    func sessionStyleLoaded(_ style: MapStyle) { listener?.onStyleLoaded(style: style) }
    func sessionMapTap(_ coordinate: GeoCoordinate) { listener?.onMapTap(coordinate: coordinate) }
    func sessionMarkerTap(_ marker: Marker) { listener?.onMarkerTap(marker: marker) }
    func sessionCameraChange(_ camera: CameraPosition, reason: CameraChangeReason) {
        listener?.onCameraChange(camera: camera, reason: reason)
    }
    func sessionCameraIdle(_ camera: CameraPosition) { listener?.onCameraIdle(camera: camera) }
}
