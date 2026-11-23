import Foundation

/// Severity of a log message. Spec: API-070.
///
/// Levels are ordered; setting ``RemaplyMaps/logLevel`` to a level drops every message below it.
/// ``none`` drops everything.
public enum LogLevel: Int, Comparable, Sendable {
    /// Very detailed tracing.
    case verbose
    /// Debugging information.
    case debug
    /// Normal operational messages.
    case info
    /// Something unexpected that the SDK recovered from. Every SDK error is logged at this level or above.
    case warn
    /// A failure the app should know about.
    case error
    /// Disables logging.
    case none

    /// Orders levels from ``verbose`` to ``none``.
    public static func < (lhs: LogLevel, rhs: LogLevel) -> Bool {
        lhs.rawValue < rhs.rawValue
    }
}

/// Receives SDK log messages. Set it with ``RemaplyMaps/logger``. Spec: API-071.
///
/// This is also the SDK's error-reporting hook: forward ``LogLevel/warn`` and ``LogLevel/error``
/// messages to your crash or analytics tool here.
///
/// **Thread safety:** called from any thread, because MapLibre logs from worker threads.
/// The logger is held strongly and globally, so do not capture a view controller in it.
public protocol MapLogger: AnyObject, Sendable {
    /// Handles one message.
    ///
    /// - Parameters:
    ///   - level: Severity.
    ///   - tag: `"Remaply"` for SDK messages, `"MapLibre"` for messages from the wrapped SDK.
    ///   - message: Human-readable text.
    ///   - error: The related error, if any.
    func log(level: LogLevel, tag: String, message: String, error: (any Error)?)
}
