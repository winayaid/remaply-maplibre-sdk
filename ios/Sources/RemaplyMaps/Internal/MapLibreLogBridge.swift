import Foundation
import MapLibre

/// Forwards MapLibre's own log output to the SDK logger with tag `"MapLibre"`.
enum MapLibreLogBridge {
    private static let installed = AtomicFlag()

    static func install() {
        guard installed.setIfUnset() else { return }
        let config = MLNLoggingConfiguration.shared
        config.loggingLevel = mapLibreLevel(for: RemaplyMaps.logLevel)
        config.handler = { level, _, _, message in
            SdkLog.log(sdkLevel(for: level), message, tag: SdkLog.mapLibreTag)
        }
    }

    static func onLogLevelChanged(_ level: LogLevel) {
        guard installed.isSet else { return }
        MLNLoggingConfiguration.shared.loggingLevel = mapLibreLevel(for: level)
    }

    /// MapLibre's release binaries have no debug/verbose levels; those map to `.info`.
    static func mapLibreLevel(for level: LogLevel) -> MLNLoggingLevel {
        switch level {
        case .verbose, .debug, .info: return .info
        case .warn: return .warning
        case .error: return .error
        case .none: return .none
        }
    }

    static func sdkLevel(for level: MLNLoggingLevel) -> LogLevel {
        switch level {
        case .fault, .error: return .error
        case .warning: return .warn
        case .info: return .info
        default: return .debug
        }
    }
}

/// A thread-safe one-shot flag.
final class AtomicFlag: @unchecked Sendable {
    private let lock = NSLock()
    private var value = false

    var isSet: Bool {
        lock.lock(); defer { lock.unlock() }
        return value
    }

    /// Sets the flag; returns `true` if it was not set before.
    func setIfUnset() -> Bool {
        lock.lock(); defer { lock.unlock() }
        if value { return false }
        value = true
        return true
    }
}
