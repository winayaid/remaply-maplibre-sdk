import MapLibre
@testable import RemaplyMaps
import UIKit
import XCTest

/// Integration tests (Phase 5): a real MapLibre map in a window, with HTTP stubbed in-process.
/// Mirrors Android's `MapInstrumentedTest`. Spec items in brackets.
@MainActor
final class MapIntegrationTests: XCTestCase {
    private var host: TestHost!
    private var listener: RecordingListener!
    private var map: RemaplyMapView!

    override func setUp() async throws {
        StubURLProtocol.install()
        host = TestHost()
        listener = RecordingListener()
        map = RemaplyMapView(style: stubStyle, options: MapOptions(initialCamera: Self.startCamera))
        map.listener = listener
        host.add(map)
        waitUntil("onMapReady") { listener.count("mapReady") == 1 }
    }

    override func tearDown() async throws {
        map.onDestroy()
        host.tearDown()
        map = nil
        host = nil
    }

    static let startCamera = CameraPosition(center: GeoCoordinate(latitude: 48.8566, longitude: 2.3522), zoom: 4)

    func testLoadsStyleAddsMarkerAndMovesCamera() throws {
        // Style loaded, then ready, with no camera events for the initial camera [API-041, §8.3].
        XCTAssertEqual(listener.log, ["styleLoaded", "mapReady"])

        // Add a marker; it is drawn on the next frame [API-019, API-006].
        try map.controller.addMarker(Marker(id: "a", coordinate: GeoCoordinate(latitude: 48.8566, longitude: 2.3522)))
        waitUntil("marker drawn") { map.renderedMarkerIds() == ["a"] }

        // Jump: exactly one change and one idle [API-016].
        listener.log.removeAll()
        try map.controller.setCamera(CameraUpdate(zoom: 6, bearing: 30))
        XCTAssertEqual(listener.log, ["change programmatic", "idle"])
        XCTAssertEqual(map.controller.camera?.zoom ?? 0, 6, accuracy: 1e-6)

        // Animate: several changes, one idle, then completion(true) [API-017].
        listener.log.removeAll()
        try map.controller.animateCamera(CameraUpdate(zoom: 8), durationMs: 300) { [listener] finished in
            listener?.log.append("completion \(finished)")
        }
        waitUntil("animation completion") { listener.count("completion") == 1 }
        XCTAssertGreaterThan(listener.log.filter { $0 == "change programmatic" }.count, 1, "\(listener.log)")
        XCTAssertEqual(Array(listener.log.suffix(2)), ["idle", "completion true"])
        XCTAssertEqual(map.controller.camera?.zoom ?? 0, 8, accuracy: 1e-6)
        XCTAssertEqual(map.renderedMarkerIds(), ["a"])
    }

    func testStyleErrorsAreReportedWithTheirCodes() throws {
        listener.log.removeAll()
        map.controller.setStyle(.url("not-a-url"))
        waitUntil("invalid url error") { listener.count("loadError") == 1 }
        map.controller.setStyle(.bundled("missing.json"))
        waitUntil("missing file error") { listener.count("loadError") == 2 }
        map.controller.setStyle(.url(stub404URL))
        waitUntil("404 error") { listener.count("loadError") == 3 }
        XCTAssertEqual(listener.log.filter { $0.hasPrefix("loadError") },
                       ["loadError invalidArgument", "loadError styleNotFound", "loadError styleLoadFailed"])
        // A working style loads again and keeps the markers [API-043].
        try map.controller.addMarker(Marker(id: "m", coordinate: GeoCoordinate(latitude: 48.8566, longitude: 2.3522)))
        map.controller.setStyle(stubStyle)
        waitUntil("style loaded") { listener.count("styleLoaded") == 1 }
        waitUntil("marker drawn") { map.renderedMarkerIds() == ["m"] }
    }

    func testMarkersKeepRenderingAfterARejectedStyleRequest() throws {
        // Phase 4 fix: a rejected request leaves the current style, which keeps rendering marker changes.
        map.controller.setStyle(.bundled("missing.json"))
        waitUntil("error") { listener.count("loadError") == 1 }
        try map.controller.addMarker(Marker(id: "d", coordinate: GeoCoordinate(latitude: 48.8566, longitude: 2.3522)))
        waitUntil("marker drawn after error") { map.renderedMarkerIds() == ["d"] }
    }

    func testANewViewOnTheSameControllerKeepsMarkersAndCamera() throws {
        // iOS counterpart of Android's recreation test [§6.6, §9.2].
        let controller = map.controller
        try controller.addMarker(Marker(id: "a", coordinate: GeoCoordinate(latitude: 48.8566, longitude: 2.3522)))
        try controller.setCamera(CameraUpdate(zoom: 7))
        map.onDestroy()
        map.removeFromSuperview()

        let second = RecordingListener()
        map = RemaplyMapView(controller: controller)
        map.listener = second
        host.add(map)
        waitUntil("onMapReady on the new view") { second.count("mapReady") == 1 }
        XCTAssertTrue(map.controller === controller)
        XCTAssertEqual(controller.markers.map(\.id), ["a"])
        XCTAssertEqual(controller.camera?.zoom ?? 0, 7, accuracy: 1e-6)
        waitUntil("marker drawn on the new view") { map.renderedMarkerIds() == ["a"] }
    }

    func testLifecycleFollowsWindowAndApp() {
        // In a window: resumed while the app is active, started otherwise.
        let attached: MapLifecycleState = UIApplication.shared.applicationState == .active ? .resumed : .started
        XCTAssertEqual(map.lifecycleState, attached)
        NotificationCenter.default.post(name: UIApplication.didBecomeActiveNotification, object: nil)
        XCTAssertEqual(map.lifecycleState, .resumed)
        NotificationCenter.default.post(name: UIApplication.willResignActiveNotification, object: nil)
        XCTAssertEqual(map.lifecycleState, .started)
        NotificationCenter.default.post(name: UIApplication.didEnterBackgroundNotification, object: nil)
        XCTAssertEqual(map.lifecycleState, .created)
        NotificationCenter.default.post(name: UIApplication.willEnterForegroundNotification, object: nil)
        XCTAssertEqual(map.lifecycleState, .started)
        map.removeFromSuperview()
        XCTAssertEqual(map.lifecycleState, .created)
        host.add(map)
        XCTAssertEqual(map.lifecycleState, attached)
        NotificationCenter.default.post(name: UIApplication.didReceiveMemoryWarningNotification, object: nil)
        let controller = map.controller
        map.onDestroy()
        XCTAssertEqual(map.lifecycleState, .destroyed)
        XCTAssertFalse(controller.isMapReady)
        XCTAssertNotNil(controller.camera) // the controller keeps its state [§6.6]
    }
}
