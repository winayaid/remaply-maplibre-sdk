@testable import RemaplyMaps
import XCTest

final class CameraMathTests: XCTestCase {
    private let eps = 1e-9

    func testBearingNormalizesIntoZeroTo360() {
        XCTAssertEqual(CameraMath.normalizeBearing(0), 0, accuracy: eps)
        XCTAssertEqual(CameraMath.normalizeBearing(360), 0, accuracy: eps)
        XCTAssertEqual(CameraMath.normalizeBearing(370), 10, accuracy: eps)
        XCTAssertEqual(CameraMath.normalizeBearing(-10), 350, accuracy: eps)
        XCTAssertEqual(CameraMath.normalizeBearing(-720), 0, accuracy: eps)
        XCTAssertEqual(CameraMath.normalizeBearing(-0.0), 0, accuracy: eps)
        XCTAssertLessThan(CameraMath.normalizeBearing(-1e-15), 360)
    }

    func testLongitudeWrapsIntoRange() {
        XCTAssertEqual(CameraMath.wrapLongitude(10), 10, accuracy: eps)
        XCTAssertEqual(CameraMath.wrapLongitude(180), 180, accuracy: eps)
        XCTAssertEqual(CameraMath.wrapLongitude(190), -170, accuracy: eps)
        XCTAssertEqual(CameraMath.wrapLongitude(-190), 170, accuracy: eps)
        XCTAssertEqual(CameraMath.wrapLongitude(720), 0, accuracy: eps)
        XCTAssertEqual(CameraMath.wrapLongitude(540), 180, accuracy: eps)
    }

    func testApplyMergesUpdateOntoCurrent() {
        let current = CameraPosition(center: coord(1, 2), zoom: 5, bearing: 10, tilt: 20)
        let result = CameraMath.apply(CameraUpdate(zoom: 7), to: current, minZoom: 0, maxZoom: 22)
        XCTAssertEqual(result, CameraPosition(center: coord(1, 2), zoom: 7, bearing: 10, tilt: 20))
    }

    func testApplyWithoutCurrentStartsFromDefault() {
        let result = CameraMath.apply(CameraUpdate(zoom: 3), to: nil, minZoom: 0, maxZoom: 22)
        XCTAssertEqual(result, CameraPosition(center: coord(0, 0), zoom: 3))
    }

    func testApplyClampsZoomAndTiltAndNormalizesBearing() {
        let result = CameraMath.apply(CameraUpdate(zoom: 30, bearing: -90, tilt: 80), to: nil, minZoom: 2, maxZoom: 18)
        XCTAssertEqual(result.zoom, 18, accuracy: eps)
        XCTAssertEqual(result.bearing, 270, accuracy: eps)
        XCTAssertEqual(result.tilt, 60, accuracy: eps)
        XCTAssertEqual(CameraMath.apply(CameraUpdate(zoom: -5), to: nil, minZoom: 2, maxZoom: 18).zoom, 2, accuracy: eps)
        XCTAssertEqual(CameraMath.apply(CameraUpdate(tilt: -5), to: nil, minZoom: 0, maxZoom: 22).tilt, 0, accuracy: eps)
    }

    func testValidateRejectsInvalidCenterAndNonFiniteNumbers() {
        assertMapError(.invalidArgument) { try CameraMath.validate(CameraUpdate(center: coord(91, 0)), api: "t") }
        assertMapError(.invalidArgument) { try CameraMath.validate(CameraUpdate(zoom: .nan), api: "t") }
        assertMapError(.invalidArgument) { try CameraMath.validate(CameraUpdate(bearing: .infinity), api: "t") }
        assertMapError(.invalidArgument) { try CameraMath.validate(CameraUpdate(tilt: -.infinity), api: "t") }
        XCTAssertNoThrow(try CameraMath.validate(CameraUpdate(zoom: 99, bearing: -999, tilt: 99), api: "t"))
    }

    func testEmptyUpdateIsDetected() {
        XCTAssertTrue(CameraMath.isEmpty(CameraUpdate()))
        XCTAssertFalse(CameraMath.isEmpty(CameraUpdate(tilt: 0)))
    }

    func testSanitizeClampsAndSwapsZoomLimits() {
        let (ok, okWarnings) = CameraMath.sanitize(MapOptions(minZoom: 3, maxZoom: 10))
        XCTAssertEqual(ok.minZoom, 3)
        XCTAssertEqual(ok.maxZoom, 10)
        XCTAssertTrue(okWarnings.isEmpty)

        let (clamped, w1) = CameraMath.sanitize(MapOptions(minZoom: -2, maxZoom: 30))
        XCTAssertEqual(clamped.minZoom, 0)
        XCTAssertEqual(clamped.maxZoom, 22)
        XCTAssertEqual(w1.count, 2)

        let (swapped, w2) = CameraMath.sanitize(MapOptions(minZoom: 12, maxZoom: 4))
        XCTAssertEqual(swapped.minZoom, 4)
        XCTAssertEqual(swapped.maxZoom, 12)
        XCTAssertEqual(w2.count, 1)
    }
}
