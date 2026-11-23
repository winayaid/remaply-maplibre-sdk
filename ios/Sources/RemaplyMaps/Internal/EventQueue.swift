import Foundation

/// Delivers callbacks in order and never re-entrantly (API_SPEC.md §8.3 rule 5).
@MainActor
final class EventQueue {
    private var pending: [() -> Void] = []
    private var draining = false

    func post(_ event: @escaping () -> Void) {
        pending.append(event)
        guard !draining else { return }
        draining = true
        defer { draining = false }
        while !pending.isEmpty {
            pending.removeFirst()()
        }
    }

    func clear() {
        pending.removeAll()
    }
}
