import SwiftUI

/// A MapLibre map for SwiftUI.
///
/// ```swift
/// @StateObject private var controller = MapController()
///
/// var body: some View {
///     RemaplyMap(style: .url("https://example.com/style.json"),
///                controller: controller,
///                onMarkerTap: { marker in /* ... */ })
///         .ignoresSafeArea()
/// }
/// ```
///
/// `style` is the source of truth: changing it loads the new style. `options` changes apply
/// immediately, except ``MapOptions/initialCamera``. The latest closures are always used.
/// Removing the view from the hierarchy destroys the native map.
///
/// Spec: API-034.
public struct RemaplyMap: View {
    private let style: MapStyle
    private let controller: MapController?
    private let options: MapOptions
    private let callbacks: RemaplyMapCallbacks
    @StateObject private var ownController = MapController()

    /// Creates a map.
    ///
    /// - Parameters:
    ///   - style: The style to show.
    ///   - controller: The map's controller. `nil` creates one kept for the view's lifetime.
    ///   - options: Map options.
    ///   - onMapReady: The first style loaded on this view and the map is ready.
    ///   - onMapLoadError: A style request failed, or the renderer could not start.
    ///   - onStyleLoaded: A style finished loading, including the first.
    ///   - onMapTap: A single tap that did not hit a marker.
    ///   - onMarkerTap: A single tap on a marker (the topmost one).
    ///   - onCameraChange: The camera changed (at most once per frame).
    ///   - onCameraIdle: The camera stopped moving.
    public init(
        style: MapStyle,
        controller: MapController? = nil,
        options: MapOptions = MapOptions(),
        onMapReady: ((MapController) -> Void)? = nil,
        onMapLoadError: ((MapError) -> Void)? = nil,
        onStyleLoaded: ((MapStyle) -> Void)? = nil,
        onMapTap: ((GeoCoordinate) -> Void)? = nil,
        onMarkerTap: ((Marker) -> Void)? = nil,
        onCameraChange: ((CameraPosition, CameraChangeReason) -> Void)? = nil,
        onCameraIdle: ((CameraPosition) -> Void)? = nil
    ) {
        self.style = style
        self.controller = controller
        self.options = options
        callbacks = RemaplyMapCallbacks(
            onMapReady: onMapReady,
            onMapLoadError: onMapLoadError,
            onStyleLoaded: onStyleLoaded,
            onMapTap: onMapTap,
            onMarkerTap: onMarkerTap,
            onCameraChange: onCameraChange,
            onCameraIdle: onCameraIdle
        )
    }

    /// The map view.
    public var body: some View {
        let resolved = controller ?? ownController
        RemaplyMapRepresentable(style: style, controller: resolved, options: options, callbacks: callbacks)
            // A different controller instance needs a new native view bound to it.
            .id(ObjectIdentifier(resolved))
    }
}

struct RemaplyMapCallbacks {
    var onMapReady: ((MapController) -> Void)?
    var onMapLoadError: ((MapError) -> Void)?
    var onStyleLoaded: ((MapStyle) -> Void)?
    var onMapTap: ((GeoCoordinate) -> Void)?
    var onMarkerTap: ((Marker) -> Void)?
    var onCameraChange: ((CameraPosition, CameraChangeReason) -> Void)?
    var onCameraIdle: ((CameraPosition) -> Void)?
}

struct RemaplyMapRepresentable: UIViewRepresentable {
    let style: MapStyle
    let controller: MapController
    let options: MapOptions
    let callbacks: RemaplyMapCallbacks

    func makeCoordinator() -> Coordinator {
        Coordinator(callbacks: callbacks)
    }

    func makeUIView(context: Context) -> RemaplyMapView {
        let view = RemaplyMapView(frame: .zero, style: style, options: options, controller: controller)
        view.listener = context.coordinator
        return view
    }

    func updateUIView(_ view: RemaplyMapView, context: Context) {
        context.coordinator.callbacks = callbacks
        if view.options != options { view.options = options }
        if controller.style != style { controller.setStyle(style) }
    }

    static func dismantleUIView(_ view: RemaplyMapView, coordinator: Coordinator) {
        view.onDestroy()
    }

    /// Forwards listener events to the latest SwiftUI closures.
    @MainActor
    final class Coordinator: MapListener {
        var callbacks: RemaplyMapCallbacks

        init(callbacks: RemaplyMapCallbacks) {
            self.callbacks = callbacks
        }

        func onMapReady(controller: MapController) { callbacks.onMapReady?(controller) }
        func onMapLoadError(error: MapError) { callbacks.onMapLoadError?(error) }
        func onStyleLoaded(style: MapStyle) { callbacks.onStyleLoaded?(style) }
        func onMapTap(coordinate: GeoCoordinate) { callbacks.onMapTap?(coordinate) }
        func onMarkerTap(marker: Marker) { callbacks.onMarkerTap?(marker) }
        func onCameraChange(camera: CameraPosition, reason: CameraChangeReason) {
            callbacks.onCameraChange?(camera, reason)
        }
        func onCameraIdle(camera: CameraPosition) { callbacks.onCameraIdle?(camera) }
    }
}
