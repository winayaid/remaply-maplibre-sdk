import Foundation

/// A complete camera: where the map looks and how.
///
/// Positions reported by the SDK are already clamped and normalized (API_SPEC.md §14).
///
/// Spec: API-002.
public struct CameraPosition: Equatable, Sendable {
    /// Geographic point at the center of the map view.
    public var center: GeoCoordinate
    /// Zoom level, where 0 shows the whole world.
    public var zoom: Double
    /// Degrees clockwise from true north, in `[0, 360)`.
    public var bearing: Double
    /// Degrees away from straight down, in `[0, 60]`.
    public var tilt: Double

    /// Creates a camera position.
    public init(center: GeoCoordinate, zoom: Double, bearing: Double = 0, tilt: Double = 0) {
        self.center = center
        self.zoom = zoom
        self.bearing = bearing
        self.tilt = tilt
    }
}

/// A partial camera change. A `nil` field keeps its current value.
///
/// Validation, applied by ``MapController/setCamera(_:)`` and
/// ``MapController/animateCamera(_:durationMs:completion:)``:
/// - an invalid `center`, or a NaN or infinite number, throws ``MapErrorCode/invalidArgument``;
/// - `zoom` is clamped to the map's zoom range and `tilt` to `[0, 60]`;
/// - `bearing` is normalized to `[0, 360)`.
///
/// An update with every field `nil` is a no-op.
///
/// Spec: API-003.
public struct CameraUpdate: Equatable, Sendable {
    /// New center, or `nil` to keep the current one.
    public var center: GeoCoordinate?
    /// New zoom level, or `nil` to keep the current one.
    public var zoom: Double?
    /// New bearing in degrees, or `nil` to keep the current one.
    public var bearing: Double?
    /// New tilt in degrees, or `nil` to keep the current one.
    public var tilt: Double?

    /// Creates a camera update.
    public init(center: GeoCoordinate? = nil, zoom: Double? = nil, bearing: Double? = nil, tilt: Double? = nil) {
        self.center = center
        self.zoom = zoom
        self.bearing = bearing
        self.tilt = tilt
    }
}

/// Why the camera changed. Spec: API-009.
public enum CameraChangeReason: Equatable, Sendable {
    /// The user moved the map: pan, pinch, rotate, tilt, double-tap, or a compass tap.
    case userGesture
    /// The app moved the map through ``MapController``, or the SDK clamped it after an options change.
    case programmatic
}
