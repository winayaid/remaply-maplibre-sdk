import Foundation

/// Lifecycle state of a map view. Spec: API-010.
public enum MapLifecycleState: Int, Comparable, Sendable {
    /// Constructed; the native map has not been created yet.
    case initialized
    /// Native map created, but not visible or not rendering.
    case created
    /// Visible and rendering.
    case started
    /// Visible, rendering, in the foreground and interactive.
    case resumed
    /// Native resources released. Terminal state.
    case destroyed

    /// Orders states from ``initialized`` to ``destroyed``.
    public static func < (lhs: MapLifecycleState, rhs: MapLifecycleState) -> Bool {
        lhs.rawValue < rhs.rawValue
    }
}
