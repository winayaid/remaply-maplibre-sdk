import Foundation

/// Global entry point for SDK-wide information and configuration.
///
/// See `api-spec/API_SPEC.md` §12 (API-080). Phase 1 provides only the version
/// properties; logging configuration (`logLevel`, `logger`) arrives in Phase 3.
public enum RemaplyMaps {
    /// Version of the Remaply Maps SDK, following semantic versioning (for example `"0.1.0"`).
    ///
    /// Must match `VERSION_NAME` in `android/gradle.properties`; `scripts/check-versions.sh` enforces this.
    public static let version: String = "0.1.0"

    /// Exact version of MapLibre Native iOS that this SDK wraps (for example `"7.0.0"`).
    ///
    /// Must match the exact version pinned in `Package.swift`.
    public static let mapLibreVersion: String = "7.0.0"
}
