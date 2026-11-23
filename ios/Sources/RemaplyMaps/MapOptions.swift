import Foundation

/// Configuration for a map view.
///
/// Every field except ``initialCamera`` can be changed at runtime and takes effect immediately.
/// Invalid zoom limits are corrected and logged at ``LogLevel/warn``: values outside `[0, 22]` are
/// clamped, and if ``minZoom`` is greater than ``maxZoom`` the two are swapped.
///
/// Spec: API-005.
public struct MapOptions: Equatable, Sendable {
    /// Applied once, when the view binds to a controller that has no stored camera.
    /// `nil` uses the style's own camera, or `(0, 0)` at zoom 0 if the style has none.
    public var initialCamera: CameraPosition?
    /// Lowest zoom level reachable by gestures and API calls.
    public var minZoom: Double
    /// Highest zoom level reachable by gestures and API calls.
    public var maxZoom: Double
    /// Whether the user can pan the map.
    public var scrollGesturesEnabled: Bool
    /// Whether the user can zoom (pinch, double-tap, two-finger tap).
    public var zoomGesturesEnabled: Bool
    /// Whether the user can rotate the map.
    public var rotateGesturesEnabled: Bool
    /// Whether the user can tilt the map.
    public var tiltGesturesEnabled: Bool
    /// Whether the compass shows (top-right) while the bearing is not 0.
    public var compassEnabled: Bool

    /// Creates map options.
    public init(
        initialCamera: CameraPosition? = nil,
        minZoom: Double = 0,
        maxZoom: Double = 22,
        scrollGesturesEnabled: Bool = true,
        zoomGesturesEnabled: Bool = true,
        rotateGesturesEnabled: Bool = true,
        tiltGesturesEnabled: Bool = true,
        compassEnabled: Bool = true
    ) {
        self.initialCamera = initialCamera
        self.minZoom = minZoom
        self.maxZoom = maxZoom
        self.scrollGesturesEnabled = scrollGesturesEnabled
        self.zoomGesturesEnabled = zoomGesturesEnabled
        self.rotateGesturesEnabled = rotateGesturesEnabled
        self.tiltGesturesEnabled = tiltGesturesEnabled
        self.compassEnabled = compassEnabled
    }
}
