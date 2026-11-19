import Foundation

/// A geographic coordinate in decimal degrees (WGS 84).
///
/// Construction never fails. Validity is checked where the value is used: passing an invalid
/// coordinate to a throwing API throws ``MapError`` with ``MapErrorCode/invalidArgument``.
/// Coordinates delivered by the SDK are always valid, with longitude wrapped into `[-180, 180]`.
///
/// Spec: API-001.
public struct GeoCoordinate: Equatable, Hashable, Sendable {
    /// Latitude in degrees, valid range `[-90, 90]`.
    public var latitude: Double
    /// Longitude in degrees, valid range `[-180, 180]`.
    public var longitude: Double

    /// Creates a coordinate.
    public init(latitude: Double, longitude: Double) {
        self.latitude = latitude
        self.longitude = longitude
    }

    /// `true` when both values are finite and within their valid ranges.
    public var isValid: Bool {
        latitude.isFinite && longitude.isFinite
            && (-90.0...90.0).contains(latitude)
            && (-180.0...180.0).contains(longitude)
    }
}
