@testable import RemaplyMaps
import XCTest

final class CameraAnimationTests: XCTestCase {
    private let eps = 1e-6

    func testEaseInOutCurveMatchesCssReferenceValues() {
        let curve = CameraAnimation.easeInOut
        XCTAssertEqual(curve.solve(0), 0, accuracy: eps)
        XCTAssertEqual(curve.solve(1), 1, accuracy: eps)
        XCTAssertEqual(curve.solve(0.5), 0.5, accuracy: 1e-4)
        XCTAssertEqual(curve.solve(0.75), 1 - curve.solve(0.25), accuracy: 1e-4)
        XCTAssertLessThan(curve.solve(0.1), 0.1)
        XCTAssertGreaterThan(curve.solve(0.9), 0.9)
        XCTAssertEqual(curve.solve(0.25), 0.1291, accuracy: 1e-3)
    }

    func testStartAndEndAreExact() {
        let from = CameraPosition(center: coord(10, 20), zoom: 3, bearing: 30, tilt: 10)
        let to = CameraPosition(center: coord(-5, 40), zoom: 9, bearing: 90, tilt: 45)
        let anim = CameraAnimation(from: from, to: to, durationMs: 1000)
        XCTAssertEqual(anim.value(atElapsedMs: 0), from)
        XCTAssertEqual(anim.value(atElapsedMs: 1000), to)
        XCTAssertEqual(anim.value(atElapsedMs: 5000), to)
        XCTAssertFalse(anim.isFinished(elapsedMs: 999))
        XCTAssertTrue(anim.isFinished(elapsedMs: 1000))
    }

    func testZeroDurationJumpsToTarget() {
        let to = CameraPosition(center: coord(1, 1), zoom: 5)
        let anim = CameraAnimation(from: CameraPosition(center: coord(0, 0), zoom: 1), to: to, durationMs: 0)
        XCTAssertEqual(anim.value(atElapsedMs: 0), to)
        XCTAssertTrue(anim.isFinished(elapsedMs: 0))
    }

    func testMidpointInterpolatesZoomTiltAndCenter() {
        let from = CameraPosition(center: coord(0, -10), zoom: 2, bearing: 0, tilt: 0)
        let to = CameraPosition(center: coord(0, 10), zoom: 4, bearing: 0, tilt: 40)
        let mid = CameraAnimation(from: from, to: to, durationMs: 1000).value(atElapsedMs: 500)
        XCTAssertEqual(mid.zoom, 3, accuracy: 1e-3)
        XCTAssertEqual(mid.tilt, 20, accuracy: 1e-3)
        XCTAssertEqual(mid.center.longitude, 0, accuracy: 1e-3)
        XCTAssertEqual(mid.center.latitude, 0, accuracy: 1e-9)
    }

    func testBearingTakesTheShorterArc() {
        let from = CameraPosition(center: coord(0, 0), zoom: 1, bearing: 350)
        let to = CameraPosition(center: coord(0, 0), zoom: 1, bearing: 10)
        let mid = CameraAnimation(from: from, to: to, durationMs: 1000).value(atElapsedMs: 500)
        XCTAssertEqual(mid.bearing, 0, accuracy: 1e-3)
    }

    func testCenterCrossesTheAntimeridianTheShortWay() {
        let from = CameraPosition(center: coord(0, 170), zoom: 1)
        let to = CameraPosition(center: coord(0, -170), zoom: 1)
        let mid = CameraAnimation(from: from, to: to, durationMs: 1000).value(atElapsedMs: 500)
        XCTAssertEqual(abs(mid.center.longitude), 180, accuracy: 1e-3)
    }

    func testMercatorRoundTrip() {
        for lat in [-80.0, -45, 0, 33.3, 80] {
            XCTAssertEqual(CameraAnimation.latitude(fromMercatorY: CameraAnimation.mercatorY(lat)), lat, accuracy: 1e-9)
        }
    }
}
