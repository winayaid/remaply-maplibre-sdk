package com.remaply.maps

/**
 * An error raised or reported by the SDK.
 *
 * [code] is part of the API contract. [message] is for humans only and may differ between
 * platforms and versions.
 *
 * Spec: API-060.
 *
 * @property code What went wrong.
 */
public class MapError(
    public val code: MapErrorCode,
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause) {
    override fun toString(): String = "MapError(code=$code, message=$message)"
}

/**
 * Error codes. Spec: API-061.
 */
public enum class MapErrorCode {
    /** An argument was invalid: a bad coordinate, number, marker id, duration, or style. */
    INVALID_ARGUMENT,

    /** `addMarker` was called with an id that already exists. */
    DUPLICATE_MARKER_ID,

    /** `updateMarker` was called with an id that does not exist. */
    MARKER_NOT_FOUND,

    /** A bundled style file is missing. */
    STYLE_NOT_FOUND,

    /** A style could not be downloaded or parsed. */
    STYLE_LOAD_FAILED,

    /** The GPU surface or renderer could not start. */
    RENDERER_INIT_FAILED,

    /** A view-level call was made after `onDestroy`. Logged only. */
    MAP_DESTROYED,

    /** An API was called from a background thread. */
    WRONG_THREAD,

    /** An unexpected failure inside the wrapped MapLibre SDK, caught and contained. */
    INTERNAL,
}
