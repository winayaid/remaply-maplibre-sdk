import Foundation

/// The controller's marker collection and its ordering rules (API_SPEC.md §5.6, §6.5).
///
/// Validation order, identical on both platforms: empty id → invalid coordinate → duplicate / not found.
struct MarkerStore {
    private struct Entry {
        var marker: Marker
        var sequence: Int
    }

    private var order: [String] = []          // insertion order; an update keeps the position
    private var entries: [String: Entry] = [:]
    private var nextSequence = 0

    var all: [Marker] { order.compactMap { entries[$0]?.marker } }
    var isEmpty: Bool { order.isEmpty }

    subscript(id: String) -> Marker? { entries[id]?.marker }

    mutating func add(_ marker: Marker) throws {
        try validate(marker, api: "addMarker")
        guard entries[marker.id] == nil else {
            throw makeMapError(.duplicateMarkerId, "addMarker: a marker with id '\(marker.id)' already exists")
        }
        entries[marker.id] = Entry(marker: marker, sequence: takeSequence())
        order.append(marker.id)
    }

    mutating func update(_ marker: Marker) throws {
        try validate(marker, api: "updateMarker")
        guard entries[marker.id] != nil else {
            throw makeMapError(.markerNotFound, "updateMarker: no marker with id '\(marker.id)'")
        }
        entries[marker.id] = Entry(marker: marker, sequence: takeSequence())
    }

    mutating func remove(_ id: String) -> Bool {
        guard entries.removeValue(forKey: id) != nil else { return false }
        order.removeAll { $0 == id }
        return true
    }

    mutating func clear() {
        order.removeAll()
        entries.removeAll()
    }

    /// Markers from bottom to top: ascending zIndex, then ascending add/update order.
    func drawOrder() -> [Marker] {
        entries.values.sorted(by: Self.isBelow).map(\.marker)
    }

    /// The topmost marker among `ids` (unknown ids are ignored), or `nil`.
    func topmost(_ ids: [String]) -> Marker? {
        ids.compactMap { entries[$0] }.max(by: Self.isBelow)?.marker
    }

    private static func isBelow(_ a: Entry, _ b: Entry) -> Bool {
        a.marker.zIndex != b.marker.zIndex ? a.marker.zIndex < b.marker.zIndex : a.sequence < b.sequence
    }

    private mutating func takeSequence() -> Int {
        defer { nextSequence += 1 }
        return nextSequence
    }

    private func validate(_ marker: Marker, api: String) throws {
        if marker.id.isEmpty { throw makeMapError(.invalidArgument, "\(api): marker id must not be empty") }
        if !marker.coordinate.isValid {
            throw makeMapError(.invalidArgument, "\(api): invalid coordinate \(marker.coordinate)")
        }
    }
}
