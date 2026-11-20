import Foundation

/// Global entry point for SDK-wide information and configuration.
///
/// No initialization call is needed. ``logLevel`` and ``logger`` are thread-safe and may be set
/// from any thread.
///
/// Spec: API-080.
public enum RemaplyMaps {
    /// Version of the Remaply Maps SDK, following semantic versioning (for example `"0.1.0"`).
    ///
    /// Must match `VERSION_NAME` in `android/gradle.properties`; `scripts/check-versions.sh` enforces this.
    public static let version: String = "0.1.0"

    /// Exact version of MapLibre Native iOS that this SDK wraps (for example `"7.0.0"`).
    ///
    /// Must match the exact version pinned in `Package.swift`.
    public static let mapLibreVersion: String = "7.0.0"

    private static let state = LoggingState()

    /// Minimum level of messages passed to ``logger``. Messages below it are dropped before the
    /// logger is called. Default: ``LogLevel/warn``.
    public static var logLevel: LogLevel {
        get { state.level }
        set {
            state.level = newValue
            MapLibreLogBridge.onLogLevelChanged(newValue)
        }
    }

    /// Receives SDK and MapLibre log messages. `nil` (the default) writes to `os.Logger`
    /// (subsystem `com.remaply.maps`). Called from any thread; see ``MapLogger``.
    public static var logger: MapLogger? {
        get { state.logger }
        set { state.logger = newValue }
    }
}

/// Lock-protected storage for the global logging configuration.
final class LoggingState: @unchecked Sendable {
    private let lock = NSLock()
    private var storedLevel: LogLevel = .warn
    private var storedLogger: MapLogger?

    var level: LogLevel {
        get { lock.lock(); defer { lock.unlock() }; return storedLevel }
        set { lock.lock(); storedLevel = newValue; lock.unlock() }
    }

    var logger: MapLogger? {
        get { lock.lock(); defer { lock.unlock() }; return storedLogger }
        set { lock.lock(); storedLogger = newValue; lock.unlock() }
    }
}
