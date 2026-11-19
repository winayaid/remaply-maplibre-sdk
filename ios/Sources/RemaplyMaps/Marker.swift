import UIKit

/// A marker drawn at a coordinate.
///
/// Markers draw above every style layer, are never hidden by symbol collision, stay upright when
/// the map rotates or tilts, and survive style changes.
///
/// Spec: API-006.
public struct Marker: Identifiable, Equatable {
    /// Non-empty and unique within a ``MapController``.
    public var id: String
    /// Where the marker is placed. Must be valid.
    public var coordinate: GeoCoordinate
    /// The image to draw.
    public var icon: MarkerIcon
    /// Which point of the icon sits on ``coordinate``.
    public var anchor: MarkerAnchor
    /// Higher values draw on top. On equal values, the most recently added or updated marker draws on top.
    public var zIndex: Int

    /// Creates a marker.
    public init(
        id: String,
        coordinate: GeoCoordinate,
        icon: MarkerIcon = .defaultPin(),
        anchor: MarkerAnchor = .bottom,
        zIndex: Int = 0
    ) {
        self.id = id
        self.coordinate = coordinate
        self.icon = icon
        self.anchor = anchor
        self.zIndex = zIndex
    }
}

/// The image drawn for a ``Marker``.
///
/// Two icons are equal when both are the default pin, or when they wrap the same `UIImage` instance.
///
/// Spec: API-007.
public struct MarkerIcon: Equatable {
    let image: UIImage?

    private init(image: UIImage?) {
        self.image = image
    }

    /// The SDK's bundled pin, 28 × 40 pt, designed for ``MarkerAnchor/bottom``.
    public static func defaultPin() -> MarkerIcon {
        MarkerIcon(image: nil)
    }

    /// A custom image, drawn at its size in points (`image.size`).
    public static func image(_ image: UIImage) -> MarkerIcon {
        MarkerIcon(image: image)
    }

    /// Identity comparison of the wrapped image.
    public static func == (lhs: MarkerIcon, rhs: MarkerIcon) -> Bool {
        lhs.image === rhs.image
    }
}

/// The point of a marker's icon that is placed at the marker's coordinate. Spec: API-008.
public enum MarkerAnchor: Equatable, Sendable, CaseIterable {
    /// Center of the icon.
    case center
    /// Middle of the top edge.
    case top
    /// Middle of the bottom edge. The default, suited to pins.
    case bottom
    /// Middle of the left edge.
    case left
    /// Middle of the right edge.
    case right
    /// Top-left corner.
    case topLeft
    /// Top-right corner.
    case topRight
    /// Bottom-left corner.
    case bottomLeft
    /// Bottom-right corner.
    case bottomRight
}
