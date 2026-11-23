import MapLibre
import UIKit

/// Renders markers as an SDK-managed shape source and symbol layer on top of the style
/// (API_SPEC.md §5.6, decision Q3). Same structure and identifiers as Android.
@MainActor
final class MarkerLayer {
    static let sourceID = "remaply-markers-source"
    static let layerID = "remaply-markers-layer"
    static let propID = "remaply_id"
    private static let propIcon = "remaply_icon"
    private static let propAnchor = "remaply_anchor"
    private static let propSort = "remaply_sort"
    private static let defaultIconID = "remaply-default-pin"
    private static let imageIDPrefix = "remaply-icon-"

    private var source: MLNShapeSource?
    // Keyed by image identity; the UIImage is kept so its identity stays valid.
    private var registeredImages: [ObjectIdentifier: (image: UIImage, name: String)] = [:]
    private var nextImageID = 0
    private lazy var defaultPin = DefaultPin.makeImage()

    /// Adds the source, layer, and default pin to a freshly loaded `style`.
    func install(in style: MLNStyle) {
        registeredImages.removeAll()
        style.setImage(defaultPin, forName: Self.defaultIconID)
        let newSource = MLNShapeSource(identifier: Self.sourceID, shape: nil, options: nil)
        style.addSource(newSource)
        let layer = MLNSymbolStyleLayer(identifier: Self.layerID, source: newSource)
        layer.iconImageName = NSExpression(forKeyPath: Self.propIcon)
        layer.iconAnchor = NSExpression(forKeyPath: Self.propAnchor)
        layer.symbolSortKey = NSExpression(forKeyPath: Self.propSort)
        layer.iconAllowsOverlap = NSExpression(forConstantValue: true)
        layer.iconIgnoresPlacement = NSExpression(forConstantValue: true)
        layer.iconRotationAlignment = NSExpression(forConstantValue: "viewport")
        layer.iconPitchAlignment = NSExpression(forConstantValue: "viewport")
        style.addLayer(layer)
        source = newSource
    }

    /// Replaces the rendered markers with `markers`, given bottom to top.
    func sync(_ markers: [Marker], in style: MLNStyle) {
        guard let source else { return }
        var used = Set<ObjectIdentifier>()
        let features: [MLNPointFeature] = markers.enumerated().map { index, marker in
            let iconName: String
            if let image = marker.icon.image {
                iconName = imageName(for: image, in: style)
                used.insert(ObjectIdentifier(image))
            } else {
                iconName = Self.defaultIconID
            }
            let feature = MLNPointFeature()
            feature.coordinate = CLLocationCoordinate2D(latitude: marker.coordinate.latitude,
                                                        longitude: marker.coordinate.longitude)
            feature.attributes = [
                Self.propID: marker.id,
                Self.propIcon: iconName,
                Self.propAnchor: Self.anchorName(marker.anchor),
                Self.propSort: index,
            ]
            return feature
        }
        // Drop images no marker uses any more, so replaced icons do not accumulate in the style.
        for (key, entry) in registeredImages where !used.contains(key) {
            style.removeImage(forName: entry.name)
            registeredImages.removeValue(forKey: key)
        }
        source.shape = MLNShapeCollectionFeature(shapes: features)
    }

    /// Forgets the current style's objects (the style itself is being discarded).
    func reset() {
        source = nil
        registeredImages.removeAll()
    }

    private func imageName(for image: UIImage, in style: MLNStyle) -> String {
        let key = ObjectIdentifier(image)
        if let entry = registeredImages[key] { return entry.name }
        let name = "\(Self.imageIDPrefix)\(nextImageID)"
        nextImageID += 1
        style.setImage(image, forName: name)
        registeredImages[key] = (image, name)
        return name
    }

    static func anchorName(_ anchor: MarkerAnchor) -> String {
        switch anchor {
        case .center: return "center"
        case .top: return "top"
        case .bottom: return "bottom"
        case .left: return "left"
        case .right: return "right"
        case .topLeft: return "top-left"
        case .topRight: return "top-right"
        case .bottomLeft: return "bottom-left"
        case .bottomRight: return "bottom-right"
        }
    }
}
