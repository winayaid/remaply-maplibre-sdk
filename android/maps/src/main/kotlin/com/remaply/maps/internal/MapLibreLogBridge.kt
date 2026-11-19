package com.remaply.maps.internal

import com.remaply.maps.LogLevel
import com.remaply.maps.RemaplyMaps
import org.maplibre.android.log.Logger
import org.maplibre.android.log.LoggerDefinition

/** Forwards MapLibre's own log output to the SDK logger with tag `"MapLibre"`. */
internal object MapLibreLogBridge : LoggerDefinition {
    @Volatile
    private var installed = false

    fun install() {
        if (installed) return
        installed = true
        Logger.setLoggerDefinition(this)
        Logger.setVerbosity(verbosityFor(RemaplyMaps.logLevel))
    }

    fun onLogLevelChanged(level: LogLevel) {
        if (installed) Logger.setVerbosity(verbosityFor(level))
    }

    fun verbosityFor(level: LogLevel): Int =
        when (level) {
            LogLevel.VERBOSE -> Logger.VERBOSE
            LogLevel.DEBUG -> Logger.DEBUG
            LogLevel.INFO -> Logger.INFO
            LogLevel.WARN -> Logger.WARN
            LogLevel.ERROR -> Logger.ERROR
            LogLevel.NONE -> Logger.NONE
        }

    private fun forward(
        level: LogLevel,
        tag: String,
        msg: String,
        tr: Throwable? = null,
    ) = SdkLog.log(level, "$tag: $msg", tr, SdkLog.MAPLIBRE_TAG)

    override fun v(
        tag: String,
        msg: String,
    ) = forward(LogLevel.VERBOSE, tag, msg)

    override fun v(
        tag: String,
        msg: String,
        tr: Throwable,
    ) = forward(LogLevel.VERBOSE, tag, msg, tr)

    override fun d(
        tag: String,
        msg: String,
    ) = forward(LogLevel.DEBUG, tag, msg)

    override fun d(
        tag: String,
        msg: String,
        tr: Throwable,
    ) = forward(LogLevel.DEBUG, tag, msg, tr)

    override fun i(
        tag: String,
        msg: String,
    ) = forward(LogLevel.INFO, tag, msg)

    override fun i(
        tag: String,
        msg: String,
        tr: Throwable,
    ) = forward(LogLevel.INFO, tag, msg, tr)

    override fun w(
        tag: String,
        msg: String,
    ) = forward(LogLevel.WARN, tag, msg)

    override fun w(
        tag: String,
        msg: String,
        tr: Throwable,
    ) = forward(LogLevel.WARN, tag, msg, tr)

    override fun e(
        tag: String,
        msg: String,
    ) = forward(LogLevel.ERROR, tag, msg)

    override fun e(
        tag: String,
        msg: String,
        tr: Throwable,
    ) = forward(LogLevel.ERROR, tag, msg, tr)
}
