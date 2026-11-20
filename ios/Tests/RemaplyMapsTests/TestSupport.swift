import MapLibre
@testable import RemaplyMaps
import XCTest

func coord(_ lat: Double, _ lng: Double) -> GeoCoordinate {
    GeoCoordinate(latitude: lat, longitude: lng)
}

/// Asserts that `block` throws a ``MapError`` with `code`.
func assertMapError(
    _ code: MapErrorCode,
    file: StaticString = #filePath,
    line: UInt = #line,
    _ block: () throws -> Void
) {
    do {
        try block()
        XCTFail("Expected MapError(\(code))", file: file, line: line)
    } catch let error as MapError {
        XCTAssertEqual(error.code, code, file: file, line: line)
    } catch {
        XCTFail("Unexpected error \(error)", file: file, line: line)
    }
}

/// A binding that records what the controller asks of it, with a synchronous "map".
@MainActor
final class FakeBinding: MapBinding {
    var ready: Bool
    var camera: CameraPosition
    let minZoom: Double
    let maxZoom: Double
    var styleRequests: [MapStyle] = []
    var jumps: [CameraPosition] = []
    var animations: [(CameraPosition, Int, ((Bool) -> Void)?)] = []
    var markerChanges = 0
    var wasUnboundByController = false

    init(ready: Bool = true,
         camera: CameraPosition = CameraPosition(center: coord(0, 0), zoom: 1),
         minZoom: Double = 0, maxZoom: Double = 22) {
        self.ready = ready
        self.camera = camera
        self.minZoom = minZoom
        self.maxZoom = maxZoom
    }

    var isReady: Bool { ready }
    var nativeMapView: MLNMapView? { nil }
    func currentCamera() -> CameraPosition { camera }
    func requestStyle(_ style: MapStyle) { styleRequests.append(style) }
    func jump(to target: CameraPosition) {
        jumps.append(target)
        camera = target
    }
    func animate(to target: CameraPosition, durationMs: Int, completion: ((Bool) -> Void)?) {
        animations.append((target, durationMs, completion))
    }
    func cancelAnimation() {}
    func markersChanged() { markerChanges += 1 }
    func unboundByController() { wasUnboundByController = true }
}

/// Captures log output.
final class CapturingLogger: MapLogger, @unchecked Sendable {
    private let lock = NSLock()
    private var stored: [(LogLevel, String, String)] = []

    var entries: [(LogLevel, String, String)] {
        lock.lock(); defer { lock.unlock() }
        return stored
    }

    func log(level: LogLevel, tag: String, message: String, error: (any Error)?) {
        lock.lock(); stored.append((level, tag, message)); lock.unlock()
    }
}
