import Combine
import MapLibre

/// The state holder and command interface of a map: style, camera, and markers.
///
/// A controller never holds a strong reference to a view, so it is safe to keep in a SwiftUI
/// `@StateObject` or a view model. It binds automatically when passed to, or created by, a
/// ``RemaplyMapView`` or ``RemaplyMap``, and keeps its state when that view goes away.
///
/// Conforms to `ObservableObject` so it can be held in `@StateObject`; it publishes nothing.
///
/// Spec: API_SPEC.md §6.
@MainActor
public final class MapController: ObservableObject {
    private var markerStore = MarkerStore()
    private var requestedStyle: MapStyle?

    /// Camera kept while no ready map exists: the last live camera, or values set while unbound.
    var storedCamera: CameraPosition?

    /// Delivers callbacks in order and without re-entrancy for this controller and its view.
    let events = EventQueue()

    /// Weak: a view that goes away without `onDestroy` must not be kept alive by its controller.
    private(set) weak var binding: MapBinding?

    /// Creates a controller with no style, camera, or markers.
    public init() {}

    /// `true` from just before `onMapReady` until the bound view is destroyed or unbound.
    /// Stays `true` while a later style loads. Spec: API-011.
    public var isMapReady: Bool {
        binding?.isReady == true
    }

    /// The most recently requested style, loaded or not. `nil` before any request. Spec: API-012.
    public var style: MapStyle? {
        requestedStyle
    }

    /// The live camera while a map is ready, otherwise the stored camera. `nil` if no camera was
    /// ever set or observed. Spec: API-013.
    public var camera: CameraPosition? {
        if let binding, binding.isReady { return binding.currentCamera() }
        return storedCamera
    }

    /// All markers, in insertion order. Spec: API-014.
    public var markers: [Marker] {
        markerStore.all
    }

    /// Requests a style load. Never throws: failures arrive in ``MapListener/onMapLoadError(error:)``.
    /// A request for the style that is already loaded or loading is a no-op. Spec: API-015.
    public func setStyle(_ style: MapStyle) {
        requestedStyle = style
        binding?.requestStyle(style)
    }

    /// Moves the camera immediately, without animation, cancelling any running animation.
    /// Spec: API-016.
    ///
    /// - Throws: ``MapError`` with ``MapErrorCode/invalidArgument`` if `update` has an invalid value.
    public func setCamera(_ update: CameraUpdate) throws {
        try CameraMath.validate(update, api: "setCamera")
        guard !CameraMath.isEmpty(update) else { return }
        if let binding, binding.isReady {
            binding.jump(to: CameraMath.apply(update, to: binding.currentCamera(),
                                              minZoom: binding.minZoom, maxZoom: binding.maxZoom))
        } else {
            storedCamera = applyUnbound(update)
        }
    }

    /// Animates the camera with an ease-in-out curve. Spec: API-017.
    ///
    /// `completion` is called exactly once: with `true` after the animation finishes (right after
    /// `onCameraIdle`), or `false` if it is cancelled by another camera call, a user touch, or the
    /// view going away. With no ready map, the stored camera changes immediately and `completion`
    /// receives `true`.
    ///
    /// - Parameters:
    ///   - update: The camera change.
    ///   - durationMs: Duration in milliseconds; 0 jumps.
    ///   - completion: Called once with whether the animation finished.
    /// - Throws: ``MapError`` with ``MapErrorCode/invalidArgument`` if `update` has an invalid value
    ///   or `durationMs` is negative.
    public func animateCamera(
        _ update: CameraUpdate,
        durationMs: Int = 500,
        completion: ((_ finished: Bool) -> Void)? = nil
    ) throws {
        try CameraMath.validate(update, api: "animateCamera")
        guard durationMs >= 0 else {
            throw makeMapError(.invalidArgument, "animateCamera: durationMs must be >= 0, was \(durationMs)")
        }
        guard !CameraMath.isEmpty(update) else {
            if let completion { events.post { completion(true) } }
            return
        }
        if let binding, binding.isReady {
            let target = CameraMath.apply(update, to: binding.currentCamera(),
                                          minZoom: binding.minZoom, maxZoom: binding.maxZoom)
            binding.animate(to: target, durationMs: durationMs, completion: completion)
        } else {
            storedCamera = applyUnbound(update)
            if let completion { events.post { completion(true) } }
        }
    }

    /// Returns the marker with `id`, or `nil`. Spec: API-018.
    public func getMarker(id: String) -> Marker? {
        markerStore[id]
    }

    /// Adds `marker`. It renders on the next frame, or once a map is ready. Spec: API-019.
    ///
    /// - Throws: ``MapErrorCode/invalidArgument`` for an empty id or invalid coordinate;
    ///   ``MapErrorCode/duplicateMarkerId`` if the id already exists.
    public func addMarker(_ marker: Marker) throws {
        try markerStore.add(marker)
        binding?.markersChanged()
    }

    /// Replaces the marker with the same id. It then counts as the most recent for draw order.
    /// Spec: API-020.
    ///
    /// - Throws: ``MapErrorCode/invalidArgument`` for an empty id or invalid coordinate;
    ///   ``MapErrorCode/markerNotFound`` if no marker has that id.
    public func updateMarker(_ marker: Marker) throws {
        try markerStore.update(marker)
        binding?.markersChanged()
    }

    /// Removes the marker with `id`. Returns `false` if there was none. Spec: API-021.
    @discardableResult
    public func removeMarker(id: String) -> Bool {
        let removed = markerStore.remove(id)
        if removed { binding?.markersChanged() }
        return removed
    }

    /// Removes all markers. Spec: API-022.
    public func removeAllMarkers() {
        guard !markerStore.isEmpty else { return }
        markerStore.clear()
        binding?.markersChanged()
    }

    /// Runs `block` synchronously with the native `MLNMapView`, only while a map is ready.
    /// Returns `false` without running it otherwise. Do not keep a reference to the view, and do
    /// not remove the SDK's marker layer or replace the style through it.
    ///
    /// - Warning: Experimental. Not covered by semantic versioning or platform parity. Spec: API-091.
    @discardableResult
    public func withNativeMap(_ block: (MLNMapView) -> Void) -> Bool {
        guard let binding, binding.isReady, let mapView = binding.nativeMapView else { return false }
        block(mapView)
        return true
    }

    // MARK: - Internal: binding (API_SPEC.md §6.6)

    func bind(_ newBinding: MapBinding) {
        let old = binding
        if old === newBinding { return }
        if let old {
            SdkLog.warn("MapController was bound to a new view while another view was still using it; the old view is unbound")
            if old.isReady { storedCamera = old.currentCamera() }
            binding = newBinding
            old.unboundByController()
        } else {
            binding = newBinding
        }
    }

    func unbind(_ oldBinding: MapBinding) {
        guard binding === oldBinding else { return }
        if oldBinding.isReady { storedCamera = oldBinding.currentCamera() }
        binding = nil
    }

    func markersInDrawOrder() -> [Marker] {
        markerStore.drawOrder()
    }

    func topmostMarker(_ ids: [String]) -> Marker? {
        markerStore.topmost(ids)
    }

    private func applyUnbound(_ update: CameraUpdate) -> CameraPosition {
        CameraMath.apply(update, to: storedCamera,
                         minZoom: binding?.minZoom ?? CameraMath.minZoom,
                         maxZoom: binding?.maxZoom ?? CameraMath.maxZoom)
    }
}
