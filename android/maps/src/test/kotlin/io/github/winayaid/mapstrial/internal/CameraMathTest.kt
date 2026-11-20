package io.github.winayaid.mapstrial.internal

import io.github.winayaid.mapstrial.CameraPosition
import io.github.winayaid.mapstrial.CameraUpdate
import io.github.winayaid.mapstrial.MapErrorCode
import io.github.winayaid.mapstrial.MapOptions
import io.github.winayaid.mapstrial.assertMapError
import io.github.winayaid.mapstrial.coord
import io.github.winayaid.mapstrial.internal.CameraMath.isEmpty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraMathTest {
    private val eps = 1e-9

    @Test
    fun bearingNormalizesIntoZeroTo360() {
        assertEquals(0.0, CameraMath.normalizeBearing(0.0), eps)
        assertEquals(0.0, CameraMath.normalizeBearing(360.0), eps)
        assertEquals(10.0, CameraMath.normalizeBearing(370.0), eps)
        assertEquals(350.0, CameraMath.normalizeBearing(-10.0), eps)
        assertEquals(0.0, CameraMath.normalizeBearing(-720.0), eps)
        assertEquals(0.0, CameraMath.normalizeBearing(-0.0), eps)
        assertTrue(CameraMath.normalizeBearing(-1e-15) < 360.0)
    }

    @Test
    fun longitudeWrapsIntoRange() {
        assertEquals(10.0, CameraMath.wrapLongitude(10.0), eps)
        assertEquals(180.0, CameraMath.wrapLongitude(180.0), eps)
        assertEquals(-170.0, CameraMath.wrapLongitude(190.0), eps)
        assertEquals(170.0, CameraMath.wrapLongitude(-190.0), eps)
        assertEquals(0.0, CameraMath.wrapLongitude(720.0), eps)
        assertEquals(180.0, CameraMath.wrapLongitude(540.0), eps)
    }

    @Test
    fun applyMergesUpdateOntoCurrent() {
        val current = CameraPosition(coord(1.0, 2.0), 5.0, 10.0, 20.0)
        val result = CameraMath.apply(current, CameraUpdate(zoom = 7.0), 0.0, 22.0)
        assertEquals(CameraPosition(coord(1.0, 2.0), 7.0, 10.0, 20.0), result)
    }

    @Test
    fun applyWithoutCurrentStartsFromDefault() {
        val result = CameraMath.apply(null, CameraUpdate(zoom = 3.0), 0.0, 22.0)
        assertEquals(CameraPosition(coord(0.0, 0.0), 3.0), result)
    }

    @Test
    fun applyClampsZoomAndTiltAndNormalizesBearing() {
        val result = CameraMath.apply(null, CameraUpdate(zoom = 30.0, bearing = -90.0, tilt = 80.0), 2.0, 18.0)
        assertEquals(18.0, result.zoom, eps)
        assertEquals(270.0, result.bearing, eps)
        assertEquals(60.0, result.tilt, eps)
        assertEquals(2.0, CameraMath.apply(null, CameraUpdate(zoom = -5.0), 2.0, 18.0).zoom, eps)
        assertEquals(0.0, CameraMath.apply(null, CameraUpdate(tilt = -5.0), 0.0, 22.0).tilt, eps)
    }

    @Test
    fun validateRejectsInvalidCenterAndNonFiniteNumbers() {
        assertMapError(
            MapErrorCode.INVALID_ARGUMENT,
        ) { CameraMath.validate(CameraUpdate(center = coord(91.0, 0.0)), "t") }
        assertMapError(MapErrorCode.INVALID_ARGUMENT) { CameraMath.validate(CameraUpdate(zoom = Double.NaN), "t") }
        assertMapError(MapErrorCode.INVALID_ARGUMENT) {
            CameraMath.validate(CameraUpdate(bearing = Double.POSITIVE_INFINITY), "t")
        }
        assertMapError(MapErrorCode.INVALID_ARGUMENT) {
            CameraMath.validate(CameraUpdate(tilt = Double.NEGATIVE_INFINITY), "t")
        }
        // Out-of-range but finite values are clamped later, not rejected.
        CameraMath.validate(CameraUpdate(zoom = 99.0, tilt = 99.0, bearing = -999.0), "t")
    }

    @Test
    fun emptyUpdateIsDetected() {
        assertTrue(CameraUpdate().isEmpty)
        assertFalse(CameraUpdate(tilt = 0.0).isEmpty)
    }

    @Test
    fun sanitizeClampsAndSwapsZoomLimits() {
        val (ok, okWarnings) = CameraMath.sanitize(MapOptions(minZoom = 3.0, maxZoom = 10.0))
        assertEquals(3.0, ok.minZoom, eps)
        assertEquals(10.0, ok.maxZoom, eps)
        assertTrue(okWarnings.isEmpty())

        val (clamped, w1) = CameraMath.sanitize(MapOptions(minZoom = -2.0, maxZoom = 30.0))
        assertEquals(0.0, clamped.minZoom, eps)
        assertEquals(22.0, clamped.maxZoom, eps)
        assertEquals(2, w1.size)

        val (swapped, w2) = CameraMath.sanitize(MapOptions(minZoom = 12.0, maxZoom = 4.0))
        assertEquals(4.0, swapped.minZoom, eps)
        assertEquals(12.0, swapped.maxZoom, eps)
        assertEquals(1, w2.size)
    }
}
