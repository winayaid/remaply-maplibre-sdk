import MapLibre
@testable import RemaplyMaps
import UIKit
import XCTest

/// Repeated create/destroy cycles must not leak (API_SPEC.md §9.4). Mirrors Android's `LeakTest`:
/// every RemaplyMapView and MLNMapView must be released, and the SDK's memory growth is compared
/// with a plain MapLibre baseline run in the same process.
@MainActor
final class LeakTests: XCTestCase {
    private static let cycles = 50
    private static let maxSDKOverheadMB = 16.0

    func testFiftyMapViewCyclesLeaveNothingBehind() {
        StubURLProtocol.install()
        let host = TestHost()
        defer { host.tearDown() }
        let controller = MapController() // as if held by a view model
        var views: [WeakBox<RemaplyMapView>] = []
        var nativeViews: [WeakBox<MLNMapView>] = []

        let baseline = measureGrowth { for _ in 0..<Self.cycles { plainMapLibreCycle(host) } }
        let sdk = measureGrowth {
            for cycle in 0..<Self.cycles { sdkCycle(host, controller, cycle, &views, &nativeViews) }
        }
        print(String(format: "LeakTests: growth over %d cycles: plain MapLibre %.1f MB, SDK %.1f MB",
                     Self.cycles, baseline, sdk))

        XCTAssertEqual(nativeViews.count, Self.cycles)
        XCTAssertEqual(views.filter { $0.value != nil }.count, 0, "RemaplyMapViews still alive")
        XCTAssertEqual(nativeViews.filter { $0.value != nil }.count, 0, "MLNMapViews still alive")
        XCTAssertFalse(controller.isMapReady)
        XCTAssertLessThan(sdk - baseline, Self.maxSDKOverheadMB,
                          String(format: "SDK growth %.1f MB vs MapLibre baseline %.1f MB", sdk, baseline))
    }

    private func sdkCycle(
        _ host: TestHost,
        _ controller: MapController,
        _ cycle: Int,
        _ views: inout [WeakBox<RemaplyMapView>],
        _ nativeViews: inout [WeakBox<MLNMapView>]
    ) {
        autoreleasepool {
            let listener = RecordingListener()
            let view = RemaplyMapView(style: stubStyle, controller: controller)
            view.listener = listener
            host.add(view, size: CGSize(width: 200, height: 200))
            waitUntil("map ready in cycle \(cycle)") { listener.count("mapReady") >= 1 }
            controller.withNativeMap { nativeViews.append(WeakBox($0)) }
            views.append(WeakBox(view))
            view.onDestroy()
            view.removeFromSuperview()
        }
        spin(0.05)
    }

    private func plainMapLibreCycle(_ host: TestHost) {
        autoreleasepool {
            let delegate = StyleLoadedDelegate()
            guard let url = URL(string: stubStyleURL) else { return }
            let view = MLNMapView(frame: CGRect(x: 0, y: 0, width: 200, height: 200), styleURL: url)
            view.delegate = delegate
            host.add(view, size: CGSize(width: 200, height: 200))
            waitUntil("plain MapLibre style") { delegate.loaded }
            view.removeFromSuperview()
        }
        spin(0.05)
    }

    private func measureGrowth(_ block: () -> Void) -> Double {
        spin(1)
        let before = memoryFootprintMB()
        block()
        spin(2)
        return memoryFootprintMB() - before
    }
}

final class WeakBox<T: AnyObject> {
    weak var value: T?
    init(_ value: T) { self.value = value }
}

private final class StyleLoadedDelegate: NSObject, MLNMapViewDelegate {
    var loaded = false
    func mapView(_ mapView: MLNMapView, didFinishLoading style: MLNStyle) { loaded = true }
}
