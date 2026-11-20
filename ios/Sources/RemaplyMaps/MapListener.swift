import Foundation

/// Receives map events. Every method has an empty default implementation.
///
/// Set it with ``RemaplyMapView/listener`` (held weakly). All callbacks run on the main thread and
/// are never re-entered: events caused by calls made inside a callback are delivered after the
/// callback returns.
///
/// Spec: API-040 to API-047.
@MainActor
public protocol MapListener: AnyObject {
    /// The first style has loaded on this view, stored markers are rendered, and the stored camera
    /// is applied. Fires once per view instance.
    func onMapReady(controller: MapController)
    /// A style request failed, or the renderer could not start. Individual tile, sprite, or glyph
    /// failures are not reported here; they are logged at ``LogLevel/warn``.
    func onMapLoadError(error: MapError)
    /// A style finished loading (including the first), after markers were re-added.
    func onStyleLoaded(style: MapStyle)
    /// A confirmed single tap that did not hit a marker.
    func onMapTap(coordinate: GeoCoordinate)
    /// A confirmed single tap on a marker. Only the topmost marker is reported, and `onMapTap` does not fire.
    func onMarkerTap(marker: Marker)
    /// The camera changed. Fires at most once per rendered frame.
    func onCameraChange(camera: CameraPosition, reason: CameraChangeReason)
    /// The camera stopped moving.
    func onCameraIdle(camera: CameraPosition)
}

/// Empty default implementations, so listeners implement only what they need.
public extension MapListener {
    /// Default: does nothing.
    func onMapReady(controller: MapController) {}
    /// Default: does nothing.
    func onMapLoadError(error: MapError) {}
    /// Default: does nothing.
    func onStyleLoaded(style: MapStyle) {}
    /// Default: does nothing.
    func onMapTap(coordinate: GeoCoordinate) {}
    /// Default: does nothing.
    func onMarkerTap(marker: Marker) {}
    /// Default: does nothing.
    func onCameraChange(camera: CameraPosition, reason: CameraChangeReason) {}
    /// Default: does nothing.
    func onCameraIdle(camera: CameraPosition) {}
}
