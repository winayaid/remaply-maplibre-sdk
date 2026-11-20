package io.github.winayaid.mapstrial

import io.github.winayaid.mapstrial.internal.MapLibreLogBridge

/**
 * Global entry point for SDK-wide information and configuration.
 *
 * No initialization call is needed: the SDK initializes MapLibre on first use.
 * [logLevel] and [logger] are thread-safe and may be set from any thread.
 *
 * Spec: API-080.
 */
public object RemaplyMaps {
    /** Version of the Remaply Maps SDK, following semantic versioning (for example `"0.1.0"`). */
    public val version: String = BuildConfig.SDK_VERSION

    /** Exact version of MapLibre Native Android that this SDK wraps (for example `"13.7.1"`). */
    public val mapLibreVersion: String = BuildConfig.MAPLIBRE_VERSION

    @Volatile
    private var currentLevel: LogLevel = LogLevel.WARN

    /**
     * Minimum level of messages passed to [logger]. Messages below it are dropped before the
     * logger is called. Default: [LogLevel.WARN].
     */
    public var logLevel: LogLevel
        get() = currentLevel
        set(value) {
            currentLevel = value
            MapLibreLogBridge.onLogLevelChanged(value)
        }

    /**
     * Receives SDK and MapLibre log messages. `null` (the default) writes to Logcat.
     *
     * Called from any thread; see [MapLogger].
     */
    @Volatile
    public var logger: MapLogger? = null
}
