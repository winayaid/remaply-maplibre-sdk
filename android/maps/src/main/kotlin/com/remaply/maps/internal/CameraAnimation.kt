package com.remaply.maps.internal

import com.remaply.maps.CameraPosition
import com.remaply.maps.GeoCoordinate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.tan

/**
 * The camera animation of API_SPEC.md §6.4, shared algorithm with iOS:
 * - ease-in-out timing, cubic-bezier(0.42, 0, 0.58, 1);
 * - center interpolated linearly in Web Mercator space (a straight line on screen), taking the
 *   shorter way around the antimeridian;
 * - zoom and tilt interpolated linearly; bearing along the shorter arc.
 */
internal class CameraAnimation(
    private val from: CameraPosition,
    private val to: CameraPosition,
    val durationMs: Long,
) {
    private val fromX = mercatorX(from.center.longitude)
    private val fromY = mercatorY(from.center.latitude)
    private val deltaX: Double =
        (mercatorX(to.center.longitude) - fromX).let { dx ->
            when {
                dx > 0.5 -> dx - 1.0
                dx < -0.5 -> dx + 1.0
                else -> dx
            }
        }
    private val deltaY = mercatorY(to.center.latitude) - fromY
    private val deltaBearing: Double =
        ((to.bearing - from.bearing) % 360.0).let { d ->
            when {
                d > 180.0 -> d - 360.0
                d < -180.0 -> d + 360.0
                else -> d
            }
        }

    /** `true` once [elapsedMs] reaches the duration. */
    fun isFinished(elapsedMs: Long): Boolean = elapsedMs >= durationMs

    /** Camera at [elapsedMs] after the start. Returns exactly [to] once finished. */
    fun valueAt(elapsedMs: Long): CameraPosition {
        if (durationMs <= 0 || elapsedMs >= durationMs) return to
        if (elapsedMs <= 0) return from
        val t = EASE_IN_OUT.solve(elapsedMs.toDouble() / durationMs)
        val x = fromX + deltaX * t
        val center =
            GeoCoordinate(
                latitude = latitudeFromMercatorY(fromY + deltaY * t),
                longitude = CameraMath.wrapLongitude(longitudeFromMercatorX(x)),
            )
        return CameraPosition(
            center = center,
            zoom = from.zoom + (to.zoom - from.zoom) * t,
            bearing = CameraMath.normalizeBearing(from.bearing + deltaBearing * t),
            tilt = from.tilt + (to.tilt - from.tilt) * t,
        )
    }

    companion object {
        val EASE_IN_OUT: UnitBezier = UnitBezier(0.42, 0.0, 0.58, 1.0)
        private const val MAX_MERCATOR_LATITUDE = 85.051128779806604

        // Normalized Web Mercator: x and y in [0, 1].
        fun mercatorX(longitude: Double): Double = (longitude + 180.0) / 360.0

        fun mercatorY(latitude: Double): Double {
            val lat = latitude.coerceIn(-MAX_MERCATOR_LATITUDE, MAX_MERCATOR_LATITUDE) * PI / 180.0
            return 0.5 - ln(tan(PI / 4 + lat / 2)) / (2 * PI)
        }

        fun longitudeFromMercatorX(x: Double): Double = x * 360.0 - 180.0

        fun latitudeFromMercatorY(y: Double): Double = (2 * atan(exp((0.5 - y) * 2 * PI)) - PI / 2) * 180.0 / PI
    }
}

/**
 * Cubic Bézier timing curve through (0,0) and (1,1), as used by CSS and Core Animation.
 * Port of the WebKit UnitBezier solver.
 */
internal class UnitBezier(
    p1x: Double,
    p1y: Double,
    p2x: Double,
    p2y: Double,
) {
    private val cx = 3.0 * p1x
    private val bx = 3.0 * (p2x - p1x) - cx
    private val ax = 1.0 - cx - bx
    private val cy = 3.0 * p1y
    private val by = 3.0 * (p2y - p1y) - cy
    private val ay = 1.0 - cy - by

    private fun sampleX(t: Double) = ((ax * t + bx) * t + cx) * t

    private fun sampleY(t: Double) = ((ay * t + by) * t + cy) * t

    private fun sampleDerivativeX(t: Double) = (3.0 * ax * t + 2.0 * bx) * t + cx

    /** Returns y for progress [x] in `[0, 1]`. */
    fun solve(x: Double): Double = sampleY(solveCurveX(x.coerceIn(0.0, 1.0)))

    private fun solveCurveX(x: Double): Double {
        // Newton's method first; fall back to bisection.
        var t = x
        repeat(8) {
            val err = sampleX(t) - x
            if (abs(err) < EPSILON) return t
            val d = sampleDerivativeX(t)
            if (abs(d) < 1e-6) return@repeat
            t -= err / d
        }
        var lo = 0.0
        var hi = 1.0
        t = x
        while (lo < hi) {
            val v = sampleX(t)
            if (abs(v - x) < EPSILON) return t
            if (x > v) lo = t else hi = t
            t = (hi - lo) / 2 + lo
            if (hi - lo < EPSILON) break
        }
        return t
    }

    private companion object {
        const val EPSILON = 1e-7
    }
}
