@testable import RemaplyMaps
import XCTest

@MainActor
final class MapControllerTests: XCTestCase {
    private var controller: MapController!

    override func setUp() async throws {
        controller = MapController()
    }

    // MARK: Unbound behavior

    func testFreshControllerHasNoState() {
        XCTAssertFalse(controller.isMapReady)
        XCTAssertNil(controller.style)
        XCTAssertNil(controller.camera)
        XCTAssertTrue(controller.markers.isEmpty)
    }

    func testSetCameraWhileUnboundUpdatesStoredCamera() throws {
        try controller.setCamera(CameraUpdate(center: coord(1, 2), zoom: 30))
        XCTAssertEqual(controller.camera, CameraPosition(center: coord(1, 2), zoom: 22))
        try controller.setCamera(CameraUpdate(bearing: 370))
        XCTAssertEqual(controller.camera?.bearing ?? -1, 10, accuracy: 1e-9)
    }

    func testAnimateWhileUnboundAppliesImmediatelyAndCompletesWithTrue() throws {
        var results: [Bool] = []
        try controller.animateCamera(CameraUpdate(zoom: 4), durationMs: 1000) { results.append($0) }
        XCTAssertEqual(results, [true])
        XCTAssertEqual(controller.camera?.zoom, 4)
    }

    func testEmptyUpdatesAreNoOps() throws {
        var results: [Bool] = []
        try controller.setCamera(CameraUpdate())
        try controller.animateCamera(CameraUpdate()) { results.append($0) }
        XCTAssertNil(controller.camera)
        XCTAssertEqual(results, [true])
    }

    func testInvalidCameraArgumentsThrowAndChangeNothing() throws {
        try controller.setCamera(CameraUpdate(zoom: 3))
        let before = controller.camera
        assertMapError(.invalidArgument) { try self.controller.setCamera(CameraUpdate(center: coord(0, 200))) }
        assertMapError(.invalidArgument) { try self.controller.animateCamera(CameraUpdate(zoom: .nan)) }
        assertMapError(.invalidArgument) { try self.controller.animateCamera(CameraUpdate(zoom: 5), durationMs: -1) }
        XCTAssertEqual(controller.camera, before)
    }

    func testSetStyleIsStoredWhileUnbound() {
        controller.setStyle(.url("https://example.com/a.json"))
        XCTAssertEqual(controller.style, .url("https://example.com/a.json"))
    }

    func testMarkerApiWhileUnbound() throws {
        try controller.addMarker(Marker(id: "a", coordinate: coord(1, 1)))
        try controller.addMarker(Marker(id: "b", coordinate: coord(2, 2)))
        XCTAssertEqual(controller.getMarker(id: "a")?.id, "a")
        assertMapError(.duplicateMarkerId) { try self.controller.addMarker(Marker(id: "a", coordinate: coord(0, 0))) }
        assertMapError(.markerNotFound) { try self.controller.updateMarker(Marker(id: "zzz", coordinate: coord(0, 0))) }
        XCTAssertFalse(controller.removeMarker(id: "zzz"))
        XCTAssertTrue(controller.removeMarker(id: "a"))
        XCTAssertEqual(controller.markers.map(\.id), ["b"])
        controller.removeAllMarkers()
        XCTAssertTrue(controller.markers.isEmpty)
    }

    // MARK: Bound behavior

    func testBoundReadyControllerForwardsToBinding() throws {
        let binding = FakeBinding()
        controller.bind(binding)
        XCTAssertTrue(controller.isMapReady)

        controller.setStyle(.bundled("s.json"))
        XCTAssertEqual(binding.styleRequests, [.bundled("s.json")])

        try controller.setCamera(CameraUpdate(zoom: 8))
        XCTAssertEqual(binding.jumps.map(\.zoom), [8])
        XCTAssertEqual(controller.camera?.zoom, 8)

        try controller.animateCamera(CameraUpdate(bearing: 45), durationMs: 250)
        XCTAssertEqual(binding.animations.count, 1)
        XCTAssertEqual(binding.animations[0].0.bearing, 45)
        XCTAssertEqual(binding.animations[0].0.zoom, 8)
        XCTAssertEqual(binding.animations[0].1, 250)

        try controller.addMarker(Marker(id: "a", coordinate: coord(0, 0)))
        try controller.updateMarker(Marker(id: "a", coordinate: coord(1, 0)))
        controller.removeMarker(id: "a")
        controller.removeMarker(id: "a")
        controller.removeAllMarkers()
        XCTAssertEqual(binding.markerChanges, 3)
    }

    func testBoundCameraIsClampedToBindingZoomRange() throws {
        let binding = FakeBinding(minZoom: 3, maxZoom: 10)
        controller.bind(binding)
        try controller.setCamera(CameraUpdate(zoom: 20))
        XCTAssertEqual(binding.jumps.map(\.zoom), [10])
    }

    func testBoundButNotReadyStoresCamera() throws {
        let binding = FakeBinding(ready: false)
        controller.bind(binding)
        try controller.setCamera(CameraUpdate(zoom: 6))
        XCTAssertTrue(binding.jumps.isEmpty)
        XCTAssertEqual(controller.camera?.zoom, 6)
    }

    func testUnbindKeepsLastLiveCamera() {
        let binding = FakeBinding(camera: CameraPosition(center: coord(5, 5), zoom: 7))
        controller.bind(binding)
        controller.unbind(binding)
        XCTAssertFalse(controller.isMapReady)
        XCTAssertEqual(controller.camera, CameraPosition(center: coord(5, 5), zoom: 7))
    }

    func testLastBindingWins() {
        let first = FakeBinding(camera: CameraPosition(center: coord(1, 1), zoom: 4))
        let second = FakeBinding(ready: false)
        controller.bind(first)
        controller.bind(second)
        XCTAssertTrue(first.wasUnboundByController)
        XCTAssertTrue(controller.binding === second)
        controller.unbind(first)
        XCTAssertTrue(controller.binding === second)
        XCTAssertEqual(controller.camera?.zoom, 4)
    }

    func testBindingIsWeakSoADeallocatedViewDoesNotLeak() {
        weak var weakBinding: FakeBinding?
        do {
            let binding = FakeBinding()
            weakBinding = binding
            controller.bind(binding)
            XCTAssertTrue(controller.isMapReady)
        }
        XCTAssertNil(weakBinding)
        XCTAssertFalse(controller.isMapReady)
    }

    // MARK: Callback ordering

    func testCompletionFromInsideACallbackIsDeferredUntilItReturns() throws {
        var log: [String] = []
        try controller.animateCamera(CameraUpdate(zoom: 2), durationMs: 100) { _ in
            log.append("first-start")
            try? self.controller.animateCamera(CameraUpdate(zoom: 3), durationMs: 100) { _ in log.append("second") }
            log.append("first-end")
        }
        XCTAssertEqual(log, ["first-start", "first-end", "second"])
    }
}
