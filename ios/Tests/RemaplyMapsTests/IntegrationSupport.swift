import MapLibre
@testable import RemaplyMaps
import UIKit
import XCTest

/// Background-only style served by ``StubURLProtocol``; it needs no tiles, sprites, or glyphs.
let stubStyleURL = "https://remaply.test/style.json"
let stub404URL = "https://remaply.test/missing-style.json"
let stubStyle = MapStyle.url(stubStyleURL)
private let stubStyleJSON = ##"""
    {"version":8,"sources":{},"layers":[{"id":"bg","type":"background","paint":{"background-color":"#DDE7EE"}}]}
    """##

/// Answers MapLibre's HTTP requests in-process, so integration tests never touch the network.
final class StubURLProtocol: URLProtocol {
    static func install() {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [StubURLProtocol.self] + (config.protocolClasses ?? [])
        MLNNetworkConfiguration.sharedManager.sessionConfiguration = config
    }

    override static func canInit(with request: URLRequest) -> Bool {
        request.url?.host == "remaply.test"
    }

    override static func canonicalRequest(for request: URLRequest) -> URLRequest { request }

    override func startLoading() {
        guard let url = request.url else { return }
        let found = url.absoluteString == stubStyleURL
        let body = Data((found ? stubStyleJSON : "not found").utf8)
        let response = HTTPURLResponse(url: url, statusCode: found ? 200 : 404, httpVersion: "HTTP/1.1",
                                       headerFields: ["Content-Type": "application/json"])
        if let response { client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed) }
        client?.urlProtocol(self, didLoad: body)
        client?.urlProtocolDidFinishLoading(self)
    }

    override func stopLoading() {}
}

/// Records listener events as text.
@MainActor
final class RecordingListener: MapListener {
    var log: [String] = []

    func count(_ prefix: String) -> Int { log.filter { $0.hasPrefix(prefix) }.count }

    func onMapReady(controller: MapController) { log.append("mapReady") }
    func onMapLoadError(error: MapError) { log.append("loadError \(error.code)") }
    func onStyleLoaded(style: MapStyle) { log.append("styleLoaded") }
    func onMapTap(coordinate: GeoCoordinate) { log.append("mapTap") }
    func onMarkerTap(marker: Marker) { log.append("markerTap \(marker.id)") }
    func onCameraChange(camera: CameraPosition, reason: CameraChangeReason) { log.append("change \(reason)") }
    func onCameraIdle(camera: CameraPosition) { log.append("idle") }
}

/// A key window to host map views, like an app screen.
@MainActor
final class TestHost {
    let window: UIWindow
    let root = UIViewController()

    init() {
        window = UIWindow(frame: CGRect(x: 0, y: 0, width: 390, height: 844))
        window.rootViewController = root
        window.makeKeyAndVisible()
    }

    func add(_ view: UIView, size: CGSize = CGSize(width: 390, height: 844)) {
        view.frame = CGRect(origin: .zero, size: size)
        root.view.addSubview(view)
    }

    func tearDown() {
        root.view.subviews.forEach { $0.removeFromSuperview() }
        window.isHidden = true
    }
}

/// Spins the main run loop until `condition` holds or `timeout` passes.
@MainActor
func waitUntil(
    _ message: String,
    timeout: TimeInterval = 15,
    file: StaticString = #filePath,
    line: UInt = #line,
    _ condition: () -> Bool
) {
    let deadline = Date().addingTimeInterval(timeout)
    while !condition() {
        if Date() > deadline {
            XCTFail("Timed out waiting for \(message)", file: file, line: line)
            return
        }
        RunLoop.main.run(until: Date().addingTimeInterval(0.05))
    }
}

/// Spins the main run loop for `seconds`.
@MainActor
func spin(_ seconds: TimeInterval) {
    RunLoop.main.run(until: Date().addingTimeInterval(seconds))
}

extension RemaplyMapView {
    /// Marker ids actually drawn on screen.
    func renderedMarkerIds() -> Set<String> {
        var ids = Set<String>()
        controller.withNativeMap { mapView in
            let features = mapView.visibleFeatures(in: mapView.bounds, styleLayerIdentifiers: [MarkerLayer.layerID])
            ids = Set(features.compactMap { $0.attribute(forKey: MarkerLayer.propID) as? String })
        }
        return ids
    }
}

/// Physical memory footprint of the process, in MB.
func memoryFootprintMB() -> Double {
    var info = task_vm_info_data_t()
    var count = mach_msg_type_number_t(MemoryLayout<task_vm_info_data_t>.size / MemoryLayout<integer_t>.size)
    let result = withUnsafeMutablePointer(to: &info) {
        $0.withMemoryRebound(to: integer_t.self, capacity: Int(count)) {
            task_info(mach_task_self_, task_flavor_t(TASK_VM_INFO), $0, &count)
        }
    }
    return result == KERN_SUCCESS ? Double(info.phys_footprint) / 1_048_576 : 0
}
