import Foundation
import os

/// Routes every SDK log message through ``RemaplyMaps/logLevel`` and ``RemaplyMaps/logger``.
enum SdkLog {
    static let tag = "Remaply"
    static let mapLibreTag = "MapLibre"

    static func isEnabled(_ level: LogLevel) -> Bool {
        level != .none && level >= RemaplyMaps.logLevel
    }

    static func log(_ level: LogLevel, _ message: String, error: (any Error)? = nil, tag: String = tag) {
        guard isEnabled(level) else { return }
        if let logger = RemaplyMaps.logger {
            logger.log(level: level, tag: tag, message: message, error: error)
        } else {
            OSLogLogger.shared.log(level: level, tag: tag, message: message, error: error)
        }
    }

    static func debug(_ message: String) { log(.debug, message) }
    static func info(_ message: String) { log(.info, message) }
    static func warn(_ message: String, error: (any Error)? = nil) { log(.warn, message, error: error) }
    static func error(_ message: String, error: (any Error)? = nil) { log(.error, message, error: error) }

    /// Logs an SDK error. Every ``MapError`` the SDK creates passes through here (API_SPEC.md §10.2).
    static func report(_ error: MapError) {
        log(error.code == .internal ? .error : .warn, "\(error.code): \(error.message)", error: error)
    }
}

/// Creates a ``MapError`` and reports it to the logger.
func makeMapError(_ code: MapErrorCode, _ message: String, cause: (any Error)? = nil) -> MapError {
    let error = MapError(code: code, message: message, cause: cause)
    SdkLog.report(error)
    return error
}

/// Default logger: `os.Logger`, subsystem `io.github.winayaid.mapstrial`, category = tag.
final class OSLogLogger: MapLogger, @unchecked Sendable {
    static let shared = OSLogLogger()
    private let sdk = Logger(subsystem: "io.github.winayaid.mapstrial", category: SdkLog.tag)
    private let mapLibre = Logger(subsystem: "io.github.winayaid.mapstrial", category: SdkLog.mapLibreTag)

    func log(level: LogLevel, tag: String, message: String, error: (any Error)?) {
        let logger = tag == SdkLog.mapLibreTag ? mapLibre : sdk
        let text = error.map { "\(message) (\($0))" } ?? message
        switch level {
        case .verbose, .debug: logger.debug("\(text, privacy: .public)")
        case .info: logger.info("\(text, privacy: .public)")
        case .warn: logger.warning("\(text, privacy: .public)")
        case .error, .none: logger.error("\(text, privacy: .public)")
        }
    }
}
