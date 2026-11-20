package io.github.winayaid.mapstrial.internal

import android.util.Log
import io.github.winayaid.mapstrial.LogLevel
import io.github.winayaid.mapstrial.MapError
import io.github.winayaid.mapstrial.MapErrorCode
import io.github.winayaid.mapstrial.MapLogger
import io.github.winayaid.mapstrial.RemaplyMaps

/** Routes every SDK log message through [RemaplyMaps.logLevel] and [RemaplyMaps.logger]. */
internal object SdkLog {
    const val TAG: String = "Remaply"
    const val MAPLIBRE_TAG: String = "MapLibre"

    fun isEnabled(level: LogLevel): Boolean = level != LogLevel.NONE && level >= RemaplyMaps.logLevel

    fun log(
        level: LogLevel,
        message: String,
        error: Throwable? = null,
        tag: String = TAG,
    ) {
        if (!isEnabled(level)) return
        val logger = RemaplyMaps.logger ?: LogcatLogger
        try {
            logger.log(level, tag, message, error)
        } catch (
            @Suppress("TooGenericExceptionCaught") t: Throwable,
        ) {
            // A faulty app logger must never break the map. Fall back to Logcat once.
            if (logger !== LogcatLogger) LogcatLogger.log(LogLevel.ERROR, TAG, "MapLogger threw", t)
        }
    }

    fun debug(message: String) = log(LogLevel.DEBUG, message)

    fun info(message: String) = log(LogLevel.INFO, message)

    fun warn(
        message: String,
        error: Throwable? = null,
    ) = log(LogLevel.WARN, message, error)

    fun error(
        message: String,
        error: Throwable? = null,
    ) = log(LogLevel.ERROR, message, error)

    /** Logs an SDK error. Every [MapError] the SDK creates passes through here (API_SPEC.md §10.2). */
    fun report(error: MapError) {
        val level = if (error.code == MapErrorCode.INTERNAL) LogLevel.ERROR else LogLevel.WARN
        log(level, "${error.code}: ${error.message}", error)
    }
}

/** Creates a [MapError] and reports it to the logger. */
internal fun mapError(
    code: MapErrorCode,
    message: String,
    cause: Throwable? = null,
): MapError = MapError(code, message, cause).also(SdkLog::report)

/** Default logger: Logcat. */
internal object LogcatLogger : MapLogger {
    override fun log(
        level: LogLevel,
        tag: String,
        message: String,
        error: Throwable?,
    ) {
        val priority =
            when (level) {
                LogLevel.VERBOSE -> Log.VERBOSE
                LogLevel.DEBUG -> Log.DEBUG
                LogLevel.INFO -> Log.INFO
                LogLevel.WARN -> Log.WARN
                LogLevel.ERROR, LogLevel.NONE -> Log.ERROR
            }
        val text = if (error != null) "$message\n${Log.getStackTraceString(error)}" else message
        Log.println(priority, tag, text)
    }
}
