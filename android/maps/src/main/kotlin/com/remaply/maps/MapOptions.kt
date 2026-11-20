package com.remaply.maps

/**
 * Configuration for a map view.
 *
 * Every field except [initialCamera] can be changed at runtime and takes effect immediately.
 * Invalid zoom limits are corrected and logged at [LogLevel.WARN]: values outside `[0, 22]` are
 * clamped, and if [minZoom] is greater than [maxZoom] the two are swapped.
 *
 * Spec: API-005.
 *
 * @property initialCamera Applied once, when the view binds to a controller that has no stored
 *   camera. `null` uses the style's own camera, or `(0, 0)` at zoom 0 if the style has none.
 * @property minZoom Lowest zoom level reachable by gestures and API calls.
 * @property maxZoom Highest zoom level reachable by gestures and API calls.
 * @property scrollGesturesEnabled Whether the user can pan the map.
 * @property zoomGesturesEnabled Whether the user can zoom (pinch, double-tap, two-finger tap).
 * @property rotateGesturesEnabled Whether the user can rotate the map.
 * @property tiltGesturesEnabled Whether the user can tilt the map.
 * @property compassEnabled Whether the compass shows (top-right) while the bearing is not 0.
 */
public data class MapOptions(
    public val initialCamera: CameraPosition? = null,
    public val minZoom: Double = 0.0,
    public val maxZoom: Double = 22.0,
    public val scrollGesturesEnabled: Boolean = true,
    public val zoomGesturesEnabled: Boolean = true,
    public val rotateGesturesEnabled: Boolean = true,
    public val tiltGesturesEnabled: Boolean = true,
    public val compassEnabled: Boolean = true,
)
