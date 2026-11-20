import Foundation

/// Camera validation, clamping, and normalization rules from API_SPEC.md §5.3 and §14.
/// Port of the Android `CameraMath`; keep both in sync.
enum CameraMath {
    static let minZoom = 0.0
    static let maxZoom = 22.0
    static let minTilt = 0.0
    static let maxTilt = 60.0

    /// Camera used when nothing else defines one: `(0, 0)` at zoom 0.
    static let defaultCamera = CameraPosition(center: GeoCoordinate(latitude: 0, longitude: 0), zoom: 0)

    static func isEmpty(_ update: CameraUpdate) -> Bool {
        update.center == nil && update.zoom == nil && update.bearing == nil && update.tilt == nil
    }

    /// Throws ``MapErrorCode/invalidArgument`` if `update` contains an invalid value.
    static func validate(_ update: CameraUpdate, api: String) throws {
        if let center = update.center, !center.isValid {
            throw makeMapError(.invalidArgument, "\(api): invalid center \(center)")
        }
        for (name, value) in [("zoom", update.zoom), ("bearing", update.bearing), ("tilt", update.tilt)] {
            if let value, !value.isFinite {
                throw makeMapError(.invalidArgument, "\(api): \(name) must be finite, was \(value)")
            }
        }
    }

    /// Normalizes `bearing` into `[0, 360)`.
    static func normalizeBearing(_ bearing: Double) -> Double {
        let r = bearing.truncatingRemainder(dividingBy: 360)
        let n = r < 0 ? r + 360 : r
        return (n >= 360 || n == 0) ? 0 : n
    }

    /// Wraps `longitude` into `[-180, 180]`. Values already in range are returned unchanged.
    static func wrapLongitude(_ longitude: Double) -> Double {
        if (-180.0...180.0).contains(longitude) { return longitude }
        let m = (longitude + 180).truncatingRemainder(dividingBy: 360)
        let w = (m < 0 ? m + 360 : m) - 180
        return (w == -180 && longitude > 0) ? 180 : w
    }

    static func clampTilt(_ tilt: Double) -> Double {
        min(max(tilt, minTilt), maxTilt)
    }

    /// Applies `update` on top of `current` (or ``defaultCamera``) and enforces all limits.
    static func apply(_ update: CameraUpdate, to current: CameraPosition?, minZoom: Double, maxZoom: Double) -> CameraPosition {
        let base = current ?? defaultCamera
        return normalize(
            CameraPosition(
                center: update.center ?? base.center,
                zoom: update.zoom ?? base.zoom,
                bearing: update.bearing ?? base.bearing,
                tilt: update.tilt ?? base.tilt
            ),
            minZoom: minZoom,
            maxZoom: maxZoom
        )
    }

    /// Clamps zoom and tilt, normalizes bearing, and wraps longitude.
    static func normalize(_ position: CameraPosition, minZoom: Double = minZoom, maxZoom: Double = maxZoom) -> CameraPosition {
        CameraPosition(
            center: GeoCoordinate(latitude: position.center.latitude, longitude: wrapLongitude(position.center.longitude)),
            zoom: min(max(position.zoom, minZoom), maxZoom),
            bearing: normalizeBearing(position.bearing),
            tilt: clampTilt(position.tilt)
        )
    }

    /// Corrects invalid zoom limits (API_SPEC.md §5.5). Returns corrected options and warnings.
    static func sanitize(_ options: MapOptions) -> (MapOptions, [String]) {
        var warnings: [String] = []
        func fix(_ name: String, _ value: Double, _ fallback: Double) -> Double {
            let fixed = value.isNaN ? fallback : min(max(value, minZoom), maxZoom)
            if fixed != value || value.isNaN {
                warnings.append("\(name) \(value) is outside [\(minZoom), \(maxZoom)]; using \(fixed)")
            }
            return fixed
        }
        var lo = fix("minZoom", options.minZoom, minZoom)
        var hi = fix("maxZoom", options.maxZoom, maxZoom)
        if lo > hi {
            warnings.append("minZoom \(lo) is greater than maxZoom \(hi); swapping them")
            swap(&lo, &hi)
        }
        var fixed = options
        fixed.minZoom = lo
        fixed.maxZoom = hi
        return (fixed, warnings)
    }
}
