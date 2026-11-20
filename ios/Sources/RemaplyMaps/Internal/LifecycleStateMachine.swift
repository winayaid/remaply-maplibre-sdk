import Foundation

/// Lifecycle steps performed by the state machine.
@MainActor
protocol LifecycleTarget: AnyObject {
    func performCreate()
    func performStart()
    func performResume()
    func performPause()
    func performStop()
    func performDestroy()
}

/// The lifecycle state machine of API_SPEC.md §9.1: idempotent and order-tolerant.
/// Port of the Android `LifecycleStateMachine`.
@MainActor
final class LifecycleStateMachine {
    private unowned let target: LifecycleTarget
    private(set) var state: MapLifecycleState = .initialized

    init(target: LifecycleTarget) {
        self.target = target
    }

    @discardableResult
    func moveUp(to desired: MapLifecycleState) -> Bool {
        if rejectIfDestroyed("move to \(desired)") { return false }
        while state < desired {
            switch state {
            case .initialized: target.performCreate(); state = .created
            case .created: target.performStart(); state = .started
            case .started: target.performResume(); state = .resumed
            case .resumed, .destroyed: return true
            }
        }
        return true
    }

    @discardableResult
    func moveDown(to desired: MapLifecycleState) -> Bool {
        if rejectIfDestroyed("move to \(desired)") { return false }
        while state > desired && state > .created {
            switch state {
            case .resumed: target.performPause(); state = .started
            case .started: target.performStop(); state = .created
            default: return true
            }
        }
        return true
    }

    func destroy() {
        if rejectIfDestroyed("destroy") { return }
        moveDown(to: .created)
        if state == .created { target.performDestroy() }
        state = .destroyed
    }

    private func rejectIfDestroyed(_ action: String) -> Bool {
        guard state == .destroyed else { return false }
        SdkLog.warn("MAP_DESTROYED: cannot \(action); the map view was already destroyed")
        return true
    }
}
