@testable import RemaplyMaps
import XCTest

final class MarkerStoreTests: XCTestCase {
    private func marker(_ id: String, z: Int = 0, lat: Double = 0) -> Marker {
        Marker(id: id, coordinate: coord(lat, 0), zIndex: z)
    }

    func testAddGetAndInsertionOrder() throws {
        var store = MarkerStore()
        try store.add(marker("a"))
        try store.add(marker("b"))
        try store.add(marker("c"))
        XCTAssertEqual(store.all.map(\.id), ["a", "b", "c"])
        XCTAssertEqual(store["b"]?.id, "b")
        XCTAssertNil(store["zzz"])
    }

    func testAddRejectsEmptyIdInvalidCoordinateAndDuplicates() throws {
        var store = MarkerStore()
        assertMapError(.invalidArgument) { try store.add(self.marker("")) }
        assertMapError(.invalidArgument) { try store.add(Marker(id: "x", coordinate: coord(100, 0))) }
        try store.add(marker("a"))
        assertMapError(.duplicateMarkerId) { try store.add(self.marker("a", lat: 5)) }
        XCTAssertEqual(store.all, [marker("a")])
    }

    func testValidationOrderIsIdThenCoordinateThenExistence() throws {
        var store = MarkerStore()
        try store.add(marker("a"))
        assertMapError(.invalidArgument) { try store.add(Marker(id: "a", coordinate: coord(100, 0))) }
        assertMapError(.invalidArgument) { try store.update(Marker(id: "zzz", coordinate: coord(100, 0))) }
    }

    func testUpdateReplacesKeepsPositionAndRejectsUnknown() throws {
        var store = MarkerStore()
        try store.add(marker("a"))
        try store.add(marker("b"))
        try store.update(marker("a", lat: 10))
        XCTAssertEqual(store.all.map(\.id), ["a", "b"])
        XCTAssertEqual(store["a"]?.coordinate.latitude, 10)
        assertMapError(.markerNotFound) { try store.update(self.marker("zzz")) }
        assertMapError(.invalidArgument) { try store.update(self.marker("")) }
    }

    func testRemoveAndClear() throws {
        var store = MarkerStore()
        try store.add(marker("a"))
        XCTAssertTrue(store.remove("a"))
        XCTAssertFalse(store.remove("a"))
        XCTAssertFalse(store.remove("zzz"))
        try store.add(marker("b"))
        store.clear()
        XCTAssertTrue(store.isEmpty)
    }

    func testDrawOrderIsZIndexThenRecency() throws {
        var store = MarkerStore()
        try store.add(marker("a"))
        try store.add(marker("b", z: 1))
        try store.add(marker("c"))
        XCTAssertEqual(store.drawOrder().map(\.id), ["a", "c", "b"])
        try store.update(marker("a"))
        XCTAssertEqual(store.drawOrder().map(\.id), ["c", "a", "b"])
    }

    func testTopmostAmongHits() throws {
        var store = MarkerStore()
        try store.add(marker("a"))
        try store.add(marker("b", z: 1))
        try store.add(marker("c"))
        XCTAssertEqual(store.topmost(["a", "b", "c"])?.id, "b")
        XCTAssertEqual(store.topmost(["a", "c"])?.id, "c")
        XCTAssertEqual(store.topmost(["a", "unknown"])?.id, "a")
        XCTAssertNil(store.topmost([]))
    }
}
