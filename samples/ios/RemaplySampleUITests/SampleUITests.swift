import XCTest

/// UI tests (Phase 5) for behavior that needs real touches: marker vs. map taps, touch-cancel of
/// animations, and rotation. Android covers the same cases in `MapInstrumentedTest`.
final class SampleUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        XCUIDevice.shared.orientation = .portrait
        app = XCUIApplication()
        // Bundled style: the style JSON is local, so taps and markers do not depend on the network.
        app.launchArguments += ["-uiTesting", "YES", "-styleAsset", "styles/demo-bundled.json"]
        app.launch()
        waitForStatus(prefix: "Map ready · 3 markers")
    }

    override func tearDownWithError() throws {
        XCUIDevice.shared.orientation = .portrait
    }

    /// The camera starts centered on marker "b" (bottom-anchored 28 × 40 pt pin): tap its body.
    private var markerB: XCUICoordinate {
        let map = app.windows.firstMatch
        return map.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).withOffset(CGVector(dx: 0, dy: -20))
    }

    private var emptyMap: XCUICoordinate {
        app.windows.firstMatch.coordinate(withNormalizedOffset: CGVector(dx: 0.25, dy: 0.8))
    }

    func testMarkerTapReportsTheMarkerAndNotTheMap() {
        markerB.tap()
        waitForStatus(prefix: "Marker tap: b")
    }

    func testTapAwayFromMarkersReportsAMapTap() {
        emptyMap.tap()
        waitForStatus(prefix: "Map tap: ")
    }

    /// Each tap must be reported on its own, without waiting for the next touch.
    func testConsecutiveMapTapsAreEachReportedPromptly() {
        let window = app.windows.firstMatch
        let status = app.staticTexts["status"]
        let points = [CGVector(dx: 0.25, dy: 0.8), CGVector(dx: 0.75, dy: 0.75), CGVector(dx: 0.3, dy: 0.6)]
        var seen: [String] = []
        for point in points {
            let before = status.label
            window.coordinate(withNormalizedOffset: point).tap()
            let changed = NSPredicate(format: "label BEGINSWITH 'Map tap: ' AND label != %@", before)
            let result = XCTWaiter().wait(for: [XCTNSPredicateExpectation(predicate: changed, object: status)], timeout: 3)
            XCTAssertEqual(result, .completed, "tap at \(point) was not reported within 3 s (status '\(status.label)')")
            seen.append(status.label)
        }
        XCTAssertEqual(Set(seen).count, points.count, "each tap reports its own coordinate: \(seen)")
    }

    func testAnimationCompletes() {
        app.buttons["Animate"].tap()
        waitForStatus(prefix: "Animation finished: true")
    }

    func testTouchDuringAnimationCancelsIt() {
        app.buttons["Animate"].tap()
        emptyMap.press(forDuration: 0.05, thenDragTo: app.windows.firstMatch.coordinate(
            withNormalizedOffset: CGVector(dx: 0.6, dy: 0.5)))
        waitForStatus(prefix: "Animation finished: false")
    }

    func testRotationKeepsMarkersAndTheyStayTappable() {
        rotate(to: .landscapeLeft)
        markerB.tap()
        waitForStatus(prefix: "Marker tap: b")
        rotate(to: .portrait)
        emptyMap.tap()
        waitForStatus(prefix: "Map tap: ")
    }

    /// Rotates and waits until the window has settled in the new orientation; UIKit ignores touches
    /// while the rotation animation runs.
    private func rotate(to orientation: UIDeviceOrientation) {
        XCUIDevice.shared.orientation = orientation
        let window = app.windows.firstMatch
        let landscape = orientation.isLandscape
        let settled = NSPredicate { _, _ in
            let frame = window.frame
            return landscape ? frame.width > frame.height : frame.height > frame.width
        }
        let result = XCTWaiter().wait(for: [XCTNSPredicateExpectation(predicate: settled, object: nil)], timeout: 10)
        XCTAssertEqual(result, .completed, "window did not rotate to \(orientation.rawValue)")
        // Let the rotation animation and the map's relayout finish.
        Thread.sleep(forTimeInterval: 1.0)
    }

    private func waitForStatus(prefix: String, timeout: TimeInterval = 20, line: UInt = #line) {
        let status = app.staticTexts["status"]
        let predicate = NSPredicate(format: "label BEGINSWITH %@", prefix)
        let expectation = XCTNSPredicateExpectation(predicate: predicate, object: status)
        let result = XCTWaiter().wait(for: [expectation], timeout: timeout)
        XCTAssertEqual(result, .completed, "status was '\(status.label)', expected prefix '\(prefix)'", line: line)
    }
}
