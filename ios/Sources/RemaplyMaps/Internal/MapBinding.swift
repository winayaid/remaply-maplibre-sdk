import MapLibre

/// What a ``MapController`` needs from the view it is bound to. Mirrors Android's `MapBinding`.
@MainActor
protocol MapBinding: AnyObject {
    var isReady: Bool { get }
    var minZoom: Double { get }
    var maxZoom: Double { get }
    func currentCamera() -> CameraPosition
    func requestStyle(_ style: MapStyle)
    func jump(to target: CameraPosition)
    func animate(to target: CameraPosition, durationMs: Int, completion: ((Bool) -> Void)?)
    func cancelAnimation()
    func markersChanged()
    func unboundByController()
    var nativeMapView: MLNMapView? { get }
}
