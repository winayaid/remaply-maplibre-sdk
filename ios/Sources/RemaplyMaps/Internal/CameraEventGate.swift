import Foundation

/// Turns raw camera movement into the event contract of API_SPEC.md §8.2–8.3:
/// one or more `onCameraChange` events, then exactly one `onCameraIdle`, with duplicates dropped.
/// Port of the Android `CameraEventGate`.
@MainActor
final class CameraEventGate {
    private let emitChange: (CameraPosition, CameraChangeReason) -> Void
    private let emitIdle: (CameraPosition) -> Void
    private var enabled = false
    private var lastEmitted: CameraPosition?
    private var inSequence = false

    init(emitChange: @escaping (CameraPosition, CameraChangeReason) -> Void,
         emitIdle: @escaping (CameraPosition) -> Void) {
        self.emitChange = emitChange
        self.emitIdle = emitIdle
    }

    var isMoving: Bool { inSequence }

    /// Starts emitting events. `current` is the baseline: no change is reported for it.
    func enable(current: CameraPosition) {
        enabled = true
        lastEmitted = current
        inSequence = false
    }

    func disable() {
        enabled = false
        inSequence = false
    }

    func change(_ position: CameraPosition, reason: CameraChangeReason) {
        guard enabled, position != lastEmitted else { return }
        lastEmitted = position
        inSequence = true
        emitChange(position, reason)
    }

    func idle(_ position: CameraPosition, reason: CameraChangeReason) {
        guard enabled else { return }
        change(position, reason: reason)
        guard inSequence else { return }
        inSequence = false
        emitIdle(position)
    }
}
