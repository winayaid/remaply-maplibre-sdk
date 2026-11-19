import MapLibre

/// The MapLibre view type that the SDK wraps.
///
/// Phase 1 uses this alias only to make sure the MapLibre dependency resolves and links in
/// every build. Phase 3 builds `RemaplyMapView` on top of it.
typealias NativeMapView = MLNMapView
