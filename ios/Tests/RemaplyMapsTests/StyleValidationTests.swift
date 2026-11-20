@testable import RemaplyMaps
import XCTest

final class StyleValidationTests: XCTestCase {
    func testAcceptsAbsoluteHttpAndHttpsUrls() {
        XCTAssertNil(StyleValidation.problem(.url("https://example.com/style.json")))
        XCTAssertNil(StyleValidation.problem(.url("http://example.com/style.json?key=abc")))
        XCTAssertNil(StyleValidation.problem(.url("HTTPS://EXAMPLE.COM/style.json")))
    }

    func testRejectsOtherUrls() {
        for url in [
            "",
            "style.json",
            "/styles/style.json",
            "ftp://example.com/style.json",
            "file:///sdcard/style.json",
            "asset://style.json",
            "https://",
            "https://exa mple.com/style.json",
        ] {
            XCTAssertEqual(StyleValidation.problem(.url(url))?.0, .invalidArgument, url)
        }
    }

    func testBundledPathRules() {
        XCTAssertNil(StyleValidation.problem(.bundled("styles/basic.json")))
        XCTAssertNil(StyleValidation.problem(.bundled("basic.json")))
        for path in ["", "   ", "../secret.json", "styles/../../x.json", "styles\\..\\x.json"] {
            XCTAssertEqual(StyleValidation.problem(.bundled(path))?.0, .invalidArgument, path)
        }
    }
}
