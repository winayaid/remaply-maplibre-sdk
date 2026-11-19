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
    func testMapLibreLinks() {
        // Creating the native view proves the MapLibre binary is linked and loadable.
        let view = NativeMapView(frame: CGRect(x: 0, y: 0, width: 100, height: 100))
        XCTAssertNotNil(view)
    }
}
