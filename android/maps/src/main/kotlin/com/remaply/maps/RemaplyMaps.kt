package com.remaply.maps

/**
 * Global entry point for SDK-wide information and configuration.
 *
 * See `api-spec/API_SPEC.md` §12 (API-080). Phase 1 provides only the version
 * properties; logging configuration (`logLevel`, `logger`) arrives in Phase 2.
 */
public object RemaplyMaps {
    /** Version of the Remaply Maps SDK, following semantic versioning (for example `"0.1.0"`). */
    public val version: String = BuildConfig.SDK_VERSION

    /** Exact version of MapLibre Native Android that this SDK wraps (for example `"13.7.1"`). */
    public val mapLibreVersion: String = BuildConfig.MAPLIBRE_VERSION
}
