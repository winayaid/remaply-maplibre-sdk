@testable import RemaplyMaps
import XCTest

final class ValueTypeTests: XCTestCase {
    func testCoordinateValidRangesAreInclusive() {
        XCTAssertTrue(coord(0, 0).isValid)
        XCTAssertTrue(coord(90, 180).isValid)
        XCTAssertTrue(coord(-90, -180).isValid)
    }

    func testCoordinateOutOfRangeOrNonFiniteIsInvalid() {
        XCTAssertFalse(coord(90.0001, 0).isValid)
        XCTAssertFalse(coord(-90.0001, 0).isValid)
        XCTAssertFalse(coord(0, 180.0001).isValid)
        XCTAssertFalse(coord(0, -180.0001).isValid)
        XCTAssertFalse(coord(.nan, 0).isValid)
        XCTAssertFalse(coord(0, .infinity).isValid)
    }

    func testDefaultPinIsASingleEqualValue() {
        XCTAssertEqual(MarkerIcon.defaultPin(), MarkerIcon.defaultPin())
        XCTAssertEqual(Marker(id: "a", coordinate: coord(0, 0)).icon, .defaultPin())
    }

    func testImageIconsCompareByIdentity() {
        let a = UIImage()
        let b = UIImage()
        XCTAssertEqual(MarkerIcon.image(a), MarkerIcon.image(a))
        XCTAssertNotEqual(MarkerIcon.image(a), MarkerIcon.image(b))
        XCTAssertNotEqual(MarkerIcon.image(a), MarkerIcon.defaultPin())
    }

    func testMarkerDefaults() {
        let m = Marker(id: "a", coordinate: coord(1, 2))
        XCTAssertEqual(m.anchor, .bottom)
        XCTAssertEqual(m.zIndex, 0)
    }

    func testOptionDefaultsMatchSpec() {
        let o = MapOptions()
        XCTAssertNil(o.initialCamera)
        XCTAssertEqual(o.minZoom, 0)
        XCTAssertEqual(o.maxZoom, 22)
        XCTAssertEqual(
            [o.scrollGesturesEnabled, o.zoomGesturesEnabled, o.rotateGesturesEnabled, o.tiltGesturesEnabled, o.compassEnabled],
            [true, true, true, true, true]
        )
    }

    func testMapErrorExposesCodeAndMessage() {
        let error = MapError(code: .styleNotFound, message: "missing")
        XCTAssertEqual(error.code, .styleNotFound)
        XCTAssertEqual(error.message, "missing")
        XCTAssertEqual(error.description, "MapError(code=styleNotFound, message=missing)")
    }

    func testLifecycleAndLogLevelsAreOrdered() {
        XCTAssertLessThan(MapLifecycleState.initialized, .created)
        XCTAssertLessThan(MapLifecycleState.resumed, .destroyed)
        XCTAssertLessThan(LogLevel.verbose, .debug)
        XCTAssertLessThan(LogLevel.error, .none)
    }
}
