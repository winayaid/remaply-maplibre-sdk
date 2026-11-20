@testable import RemaplyMaps
import XCTest

final class LoggingTests: XCTestCase {
    private let capture = CapturingLogger()

    override func setUp() {
        RemaplyMaps.logger = capture
        RemaplyMaps.logLevel = .warn
    }

    override func tearDown() {
        RemaplyMaps.logger = nil
        RemaplyMaps.logLevel = .warn
    }

    func testDefaultLevelIsWarn() {
        RemaplyMaps.logger = nil
        XCTAssertEqual(RemaplyMaps.logLevel, .warn)
    }

    func testMessagesBelowTheLevelAreDropped() {
        SdkLog.debug("d")
        SdkLog.info("i")
        SdkLog.warn("w")
        SdkLog.error("e")
        XCTAssertEqual(capture.entries.map(\.0), [.warn, .error])
        XCTAssertTrue(capture.entries.allSatisfy { $0.1 == "Remaply" })
    }

    func testNoneDisablesEverything() {
        RemaplyMaps.logLevel = .none
        SdkLog.error("e")
        XCTAssertTrue(capture.entries.isEmpty)
    }

    func testEverySdkErrorIsReported() {
        _ = makeMapError(.markerNotFound, "nope")
        XCTAssertEqual(capture.entries.count, 1)
        XCTAssertEqual(capture.entries.first?.0, .warn)
        XCTAssertTrue(capture.entries.first?.2.contains("markerNotFound") ?? false)
    }

    func testMapLibreLevelMapping() {
        XCTAssertEqual(MapLibreLogBridge.mapLibreLevel(for: .verbose), .info)
        XCTAssertEqual(MapLibreLogBridge.mapLibreLevel(for: .warn), .warning)
        XCTAssertEqual(MapLibreLogBridge.mapLibreLevel(for: .none), .none)
        XCTAssertEqual(MapLibreLogBridge.sdkLevel(for: .fault), .error)
        XCTAssertEqual(MapLibreLogBridge.sdkLevel(for: .warning), .warn)
    }
}
