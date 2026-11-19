import Foundation

/// An error raised or reported by the SDK.
///
/// ``code`` is part of the API contract. ``message`` is for humans only and may differ between
/// platforms and versions.
///
/// Spec: API-060.
public struct MapError: Error, CustomStringConvertible, @unchecked Sendable {
    /// What went wrong.
    public let code: MapErrorCode
    /// Human-readable description.
    public let message: String
    /// The underlying error, if any.
    public let cause: (any Error)?

    /// Creates an error.
    public init(code: MapErrorCode, message: String, cause: (any Error)? = nil) {
        self.code = code
        self.message = message
        self.cause = cause
    }

    /// `MapError(code=…, message=…)`, matching Android.
    public var description: String {
        "MapError(code=\(code), message=\(message))"
    }
}

/// Error codes. Spec: API-061.
public enum MapErrorCode: Equatable, Sendable, CaseIterable {
    /// An argument was invalid: a bad coordinate, number, marker id, duration, or style.
    case invalidArgument
    /// `addMarker` was called with an id that already exists.
    case duplicateMarkerId
    /// `updateMarker` was called with an id that does not exist.
    case markerNotFound
    /// A bundled style file is missing.
    case styleNotFound
    /// A style could not be downloaded or parsed.
    case styleLoadFailed
    /// The GPU surface or renderer could not start.
    case rendererInitFailed
    /// A view-level call was made after `onDestroy`. Logged only.
    case mapDestroyed
    /// An API was called from a background thread (Android only; prevented at compile time on iOS).
    case wrongThread
    /// An unexpected failure inside the wrapped MapLibre SDK, caught and contained.
    case `internal`
}
