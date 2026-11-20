package io.github.winayaid.mapstrial.internal

import io.github.winayaid.mapstrial.CameraPosition
import io.github.winayaid.mapstrial.CameraUpdate
import io.github.winayaid.mapstrial.GeoCoordinate
import io.github.winayaid.mapstrial.MapError
import io.github.winayaid.mapstrial.MapErrorCode
import io.github.winayaid.mapstrial.MapOptions

/** Camera validation, clamping, and normalization rules from API_SPEC.md §5.3 and §14. */
internal object CameraMath {
    const val MIN_ZOOM: Double = 0.0
    const val MAX_ZOOM: Double = 22.0
    const val MIN_TILT: Double = 0.0
    const val MAX_TILT: Double = 60.0

    /** Camera used when nothing else defines one: `(0, 0)` at zoom 0. */
    val DEFAULT_CAMERA: CameraPosition = CameraPosition(GeoCoordinate(0.0, 0.0), 0.0)

    val CameraUpdate.isEmpty: Boolean
        get() = center == null && zoom == null && bearing == null && tilt == null

    /** Throws [MapErrorCode.INVALID_ARGUMENT] if [update] contains an invalid value. */
    @Throws(MapError::class)
    fun validate(
        update: CameraUpdate,
        api: String,
    ) {
        update.center?.let {
            if (!it.isValid) throw mapError(MapErrorCode.INVALID_ARGUMENT, "$api: invalid center $it")
        }
        listOf("zoom" to update.zoom, "bearing" to update.bearing, "tilt" to update.tilt).forEach { (name, value) ->
            if (value != null && !value.isFinite()) {
                throw mapError(MapErrorCode.INVALID_ARGUMENT, "$api: $name must be finite, was $value")
            }
        }
    }

    /** Normalizes [bearing] into `[0, 360)`. */
    fun normalizeBearing(bearing: Double): Double {
        val r = bearing % 360.0
        val n = if (r < 0) r + 360.0 else r
        return if (n >= 360.0 || n == -0.0) 0.0 else n
    }

    /** Wraps [longitude] into `[-180, 180]`. Values already in range are returned unchanged. */
    fun wrapLongitude(longitude: Double): Double {
        if (longitude in -180.0..180.0) return longitude
        val w = ((longitude + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
        return if (w == -180.0 && longitude > 0) 180.0 else w
    }

    fun clampTilt(tilt: Double): Double = tilt.coerceIn(MIN_TILT, MAX_TILT)

    /** Applies [update] on top of [current] (or [DEFAULT_CAMERA]) and enforces all limits. */
    fun apply(
        current: CameraPosition?,
        update: CameraUpdate,
        minZoom: Double,
        maxZoom: Double,
    ): CameraPosition {
        val base = current ?: DEFAULT_CAMERA
        return normalize(
            CameraPosition(
                center = update.center ?: base.center,
                zoom = update.zoom ?: base.zoom,
                bearing = update.bearing ?: base.bearing,
                tilt = update.tilt ?: base.tilt,
            ),
            minZoom,
            maxZoom,
        )
    }

    /** Clamps zoom and tilt, normalizes bearing, and wraps longitude. */
    fun normalize(
        position: CameraPosition,
        minZoom: Double = MIN_ZOOM,
        maxZoom: Double = MAX_ZOOM,
    ): CameraPosition =
        CameraPosition(
            center = GeoCoordinate(position.center.latitude, wrapLongitude(position.center.longitude)),
            zoom = position.zoom.coerceIn(minZoom, maxZoom),
            bearing = normalizeBearing(position.bearing),
            tilt = clampTilt(position.tilt),
        )

    /**
     * Corrects invalid zoom limits (API_SPEC.md §5.5): clamps them into `[0, 22]` and swaps them
     * if min > max. Returns the corrected options and a description of each correction.
     */
    fun sanitize(options: MapOptions): Pair<MapOptions, List<String>> {
        val warnings = mutableListOf<String>()

        fun fix(
            name: String,
            value: Double,
            fallback: Double,
        ): Double {
            val fixed = if (value.isNaN()) fallback else value.coerceIn(MIN_ZOOM, MAX_ZOOM)
            if (fixed != value) warnings += "$name $value is outside [$MIN_ZOOM, $MAX_ZOOM]; using $fixed"
            return fixed
        }
        var min = fix("minZoom", options.minZoom, MIN_ZOOM)
        var max = fix("maxZoom", options.maxZoom, MAX_ZOOM)
        if (min > max) {
            warnings += "minZoom $min is greater than maxZoom $max; swapping them"
            min = max.also { max = min }
        }
        return options.copy(minZoom = min, maxZoom = max) to warnings
    }
}
