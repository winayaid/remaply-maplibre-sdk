import Foundation

/// The camera animation of API_SPEC.md §6.4, shared algorithm with Android:
/// ease-in-out (cubic-bezier 0.42, 0, 0.58, 1); center linear in Web Mercator space taking the
/// shorter way across the antimeridian; zoom and tilt linear; bearing along the shorter arc.
struct CameraAnimation {
    static let easeInOut = UnitBezier(p1x: 0.42, p1y: 0, p2x: 0.58, p2y: 1)
    private static let maxMercatorLatitude = 85.051128779806604

    let from: CameraPosition
    let to: CameraPosition
    let durationMs: Int

    private let fromX: Double
    private let fromY: Double
    private let deltaX: Double
    private let deltaY: Double
    private let deltaBearing: Double

    init(from: CameraPosition, to: CameraPosition, durationMs: Int) {
        self.from = from
        self.to = to
        self.durationMs = durationMs
        fromX = Self.mercatorX(from.center.longitude)
        fromY = Self.mercatorY(from.center.latitude)
        var dx = Self.mercatorX(to.center.longitude) - fromX
        if dx > 0.5 { dx -= 1 } else if dx < -0.5 { dx += 1 }
        deltaX = dx
        deltaY = Self.mercatorY(to.center.latitude) - fromY
        var db = (to.bearing - from.bearing).truncatingRemainder(dividingBy: 360)
        if db > 180 { db -= 360 } else if db < -180 { db += 360 }
        deltaBearing = db
    }

    func isFinished(elapsedMs: Int) -> Bool { elapsedMs >= durationMs }

    /// Camera at `elapsedMs` after the start. Returns exactly `to` once finished.
    func value(atElapsedMs elapsedMs: Int) -> CameraPosition {
        if durationMs <= 0 || elapsedMs >= durationMs { return to }
        if elapsedMs <= 0 { return from }
        let t = Self.easeInOut.solve(Double(elapsedMs) / Double(durationMs))
        let center = GeoCoordinate(
            latitude: Self.latitude(fromMercatorY: fromY + deltaY * t),
            longitude: CameraMath.wrapLongitude(Self.longitude(fromMercatorX: fromX + deltaX * t))
        )
        return CameraPosition(
            center: center,
            zoom: from.zoom + (to.zoom - from.zoom) * t,
            bearing: CameraMath.normalizeBearing(from.bearing + deltaBearing * t),
            tilt: from.tilt + (to.tilt - from.tilt) * t
        )
    }

    // Normalized Web Mercator: x and y in [0, 1].
    static func mercatorX(_ longitude: Double) -> Double { (longitude + 180) / 360 }

    static func mercatorY(_ latitude: Double) -> Double {
        let lat = min(max(latitude, -maxMercatorLatitude), maxMercatorLatitude) * .pi / 180
        return 0.5 - log(tan(.pi / 4 + lat / 2)) / (2 * .pi)
    }

    static func longitude(fromMercatorX x: Double) -> Double { x * 360 - 180 }

    static func latitude(fromMercatorY y: Double) -> Double {
        (2 * atan(exp((0.5 - y) * 2 * .pi)) - .pi / 2) * 180 / .pi
    }
}

/// Cubic Bézier timing curve through (0,0) and (1,1), as used by CSS and Core Animation.
/// Port of the WebKit UnitBezier solver (same as Android).
struct UnitBezier {
    private let ax, bx, cx, ay, by, cy: Double
    private static let epsilon = 1e-7

    init(p1x: Double, p1y: Double, p2x: Double, p2y: Double) {
        cx = 3 * p1x
        bx = 3 * (p2x - p1x) - cx
        ax = 1 - cx - bx
        cy = 3 * p1y
        by = 3 * (p2y - p1y) - cy
        ay = 1 - cy - by
    }

    private func sampleX(_ t: Double) -> Double { ((ax * t + bx) * t + cx) * t }
    private func sampleY(_ t: Double) -> Double { ((ay * t + by) * t + cy) * t }
    private func sampleDerivativeX(_ t: Double) -> Double { (3 * ax * t + 2 * bx) * t + cx }

    /// Returns y for progress `x` in `[0, 1]`.
    func solve(_ x: Double) -> Double { sampleY(solveCurveX(min(max(x, 0), 1))) }

    private func solveCurveX(_ x: Double) -> Double {
        var t = x
        for _ in 0..<8 {
            let err = sampleX(t) - x
            if abs(err) < Self.epsilon { return t }
            let d = sampleDerivativeX(t)
            if abs(d) < 1e-6 { break }
            t -= err / d
        }
        var lo = 0.0, hi = 1.0
        t = x
        while lo < hi {
            let v = sampleX(t)
            if abs(v - x) < Self.epsilon { return t }
            if x > v { lo = t } else { hi = t }
            t = (hi - lo) / 2 + lo
            if hi - lo < Self.epsilon { break }
        }
        return t
    }
}
