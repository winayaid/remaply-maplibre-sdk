import QuartzCore
import UIKit

/// Calls `onFrame` once per display frame with the frame timestamp (seconds), until `stop()`.
///
/// The display link targets a weak proxy, so a driver that is released without `stop()` does not
/// leak: the link invalidates itself on its next tick.
@MainActor
final class FrameDriver {
    private var link: CADisplayLink?
    private let onFrame: (CFTimeInterval) -> Void

    init(onFrame: @escaping (CFTimeInterval) -> Void) {
        self.onFrame = onFrame
    }

    func start() {
        guard link == nil else { return }
        let proxy = WeakFrameProxy(owner: self)
        let newLink = CADisplayLink(target: proxy, selector: #selector(WeakFrameProxy.tick(_:)))
        newLink.add(to: .main, forMode: .common)
        link = newLink
    }

    func stop() {
        link?.invalidate()
        link = nil
    }

    fileprivate func tick(_ timestamp: CFTimeInterval) {
        onFrame(timestamp)
    }
}

private final class WeakFrameProxy: NSObject {
    weak var owner: FrameDriver?

    init(owner: FrameDriver) {
        self.owner = owner
    }

    @MainActor @objc func tick(_ link: CADisplayLink) {
        guard let owner else {
            link.invalidate()
            return
        }
        owner.tick(link.timestamp)
    }
}
