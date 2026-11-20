import XCTest

/// Opens every sample from the launcher and exercises it (Phase 6: "every sample runs").
/// Android runs the same checks in `SampleScreensTest`.
final class SampleScreensUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        XCUIDevice.shared.orientation = .portrait
        app = XCUIApplication()
        // Bundled style: the style JSON is local, so the screens do not depend on the network to load.
        app.launchArguments += ["-sampleStyleAsset", "styles/demo-bundled.json"]
        app.launch()
    }

    func testBasicMap() {
        open("Basic map")
        waitForStatus(prefix: "Map ready.")
    }

    func testMarkersAndTaps() {
        open("Markers and taps")
        waitForStatus(prefix: "Tap the map to add a marker.")
        tapMap(dx: 0.3, dy: 0.7)
        waitForStatus(prefix: "Added m1 ")
        tapMap(dx: 0.7, dy: 0.75)
        waitForStatus(prefix: "Added m2 ")
        XCTAssertTrue(status.label.hasSuffix("2 markers"), status.label)
        app.buttons["Clear"].tap()
        waitForStatus(prefix: "All markers removed.")
    }

    func testCameraAnimation() {
        open("Camera animation")
        waitForStatus(prefix: "Pick a destination.")
        app.buttons["Tokyo"].tap()
        waitForStatus(prefix: "Arrived at Tokyo")
        app.buttons["Rotate 90°"].tap()
        waitForStatus(prefix: "Rotated to 90°")
        app.buttons["Jump to world"].tap()
        waitForStatus(prefix: "Jumped")
    }

    func testSwiftUIExample() {
        open("SwiftUI example")
        waitForStatus(prefix: "Map ready · 3 markers")
        app.buttons["Animate"].tap()
        waitForStatus(prefix: "Animation finished: true")
    }

    func testUIKitExample() {
        open("UIKit example")
        waitForStatus(prefix: "Map ready · 3 markers")
        app.buttons["Animate"].tap()
        waitForStatus(prefix: "Animation finished: true")
        app.buttons["Close"].tap()
        XCTAssertTrue(app.navigationBars["Remaply Maps"].waitForExistence(timeout: 5))
    }

    // MARK: Helpers

    private var status: XCUIElement { app.staticTexts["status"] }

    private func open(_ sample: String) {
        let row = app.buttons.containing(NSPredicate(format: "label BEGINSWITH %@", sample)).firstMatch
        XCTAssertTrue(row.waitForExistence(timeout: 10), "launcher row '\(sample)' not found")
        row.tap()
    }

    private func tapMap(dx: CGFloat, dy: CGFloat) {
        app.windows.firstMatch.coordinate(withNormalizedOffset: CGVector(dx: dx, dy: dy)).tap()
    }

    private func waitForStatus(prefix: String, timeout: TimeInterval = 20, line: UInt = #line) {
        let predicate = NSPredicate(format: "label BEGINSWITH %@", prefix)
        let result = XCTWaiter().wait(for: [XCTNSPredicateExpectation(predicate: predicate, object: status)],
                                      timeout: timeout)
        XCTAssertEqual(result, .completed, "status was '\(status.label)', expected prefix '\(prefix)'", line: line)
    }
}
