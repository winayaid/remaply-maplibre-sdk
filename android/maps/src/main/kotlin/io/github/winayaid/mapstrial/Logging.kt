package io.github.winayaid.mapstrial

/**
 * Severity of a log message. Spec: API-070.
 *
 * Levels are ordered; setting [RemaplyMaps.logLevel] to a level drops every message below it.
 * [NONE] drops everything.
 */
public enum class LogLevel {
    /** Very detailed tracing. */
    VERBOSE,

    /** Debugging information. */
    DEBUG,

    /** Normal operational messages. */
    INFO,

    /** Something unexpected that the SDK recovered from. Every SDK error is logged at this level or above. */
    WARN,

    /** A failure the app should know about. */
    ERROR,

    /** Disables logging. */
    NONE,
}

/**
 * Receives SDK log messages. Set it with [RemaplyMaps.logger]. Spec: API-071.
 *
 * This is also the SDK's error-reporting hook: forward [LogLevel.WARN] and [LogLevel.ERROR]
 * messages to your crash or analytics tool here.
 *
 * **Thread safety:** called from any thread, because MapLibre logs from worker threads.
 * The logger is held strongly and globally, so do not capture an Activity in it.
 */
public fun interface MapLogger {
    /**
     * Handles one message.
     *
     * @param level Severity.
     * @param tag `"Remaply"` for SDK messages, `"MapLibre"` for messages from the wrapped SDK.
     * @param message Human-readable text.
     * @param error The related error, if any.
     */
    public fun log(
        level: LogLevel,
        tag: String,
        message: String,
        error: Throwable?,
    )
}
