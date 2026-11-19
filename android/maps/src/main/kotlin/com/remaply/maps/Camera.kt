package com.remaply.maps

/**
 * A complete camera: where the map looks and how.
 *
 * Positions reported by the SDK are already clamped and normalized (API_SPEC.md §14).
 *
 * Spec: API-002.
 *
 * @property center Geographic point at the center of the map view.
 * @property zoom Zoom level, where 0 shows the whole world.
 * @property bearing Degrees clockwise from true north, in `[0, 360)`.
 * @property tilt Degrees away from straight down, in `[0, 60]`.
 */
public data class CameraPosition(
    public val center: GeoCoordinate,
    public val zoom: Double,
    public val bearing: Double = 0.0,
    public val tilt: Double = 0.0,
)

/**
 * A partial camera change. A `null` field keeps its current value.
 *
 * Validation, applied by [MapController.setCamera] and [MapController.animateCamera]:
 * - an invalid [center], or a NaN or infinite number, throws [MapErrorCode.INVALID_ARGUMENT];
 * - [zoom] is clamped to the map's zoom range and [tilt] to `[0, 60]`;
 * - [bearing] is normalized to `[0, 360)`.
 *
 * An update with every field `null` is a no-op.
 *
 * Spec: API-003.
 */
public data class CameraUpdate(
    public val center: GeoCoordinate? = null,
    public val zoom: Double? = null,
    public val bearing: Double? = null,
    public val tilt: Double? = null,
)

/**
 * Why the camera changed. Spec: API-009.
 */
public enum class CameraChangeReason {
    /** The user moved the map: pan, pinch, rotate, tilt, double-tap, or a compass tap. */
    USER_GESTURE,

    /** The app moved the map through [MapController], or the SDK clamped it after an options change. */
    PROGRAMMATIC,
}
