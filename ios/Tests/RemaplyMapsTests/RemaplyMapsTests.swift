@testable import RemaplyMaps
import XCTest

final class RemaplyMapsTests: XCTestCase {
    func testVersionIsSemVer() throws {
        let semVer = try NSRegularExpression(pattern: #"^\d+\.\d+\.\d+(-[0-9A-Za-z.-]+)?$"#)
        let version = RemaplyMaps.version
        let range = NSRange(version.startIndex..., in: version)
        XCTAssertNotNil(semVer.firstMatch(in: version, range: range), "version '\(version)' is not SemVer")
    }

    func testMapLibreVersionIsPinnedRelease() {
        // Pinned per API_SPEC.md §1. A prerelease or "-globe" build must never be used.
        XCTAssertEqual(RemaplyMaps.mapLibreVersion, "7.0.0")
    }

    @MainActor
    func testRemaplyMapViewCreatesAndDestroysCleanly() {
        // Creating the view proves the MapLibre binary is linked and loadable.
        weak var weakView: RemaplyMapView?
        let controller = MapController()
        autoreleasepool {
            let view = RemaplyMapView(frame: CGRect(x: 0, y: 0, width: 100, height: 100),
                                      style: .url("https://example.com/style.json"), controller: controller)
            weakView = view
            view.onCreate()
            XCTAssertEqual(view.lifecycleState, .created)
            view.onDestroy()
            XCTAssertEqual(view.lifecycleState, .destroyed)
        }
        XCTAssertNil(weakView, "RemaplyMapView leaked after onDestroy")
        XCTAssertEqual(controller.style, .url("https://example.com/style.json"))
        XCTAssertFalse(controller.isMapReady)
    }
}
