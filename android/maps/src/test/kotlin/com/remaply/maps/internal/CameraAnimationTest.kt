package com.remaply.maps.internal

import com.remaply.maps.CameraPosition
import com.remaply.maps.coord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraAnimationTest {
    private val eps = 1e-6

    @Test
    fun easeInOutCurveMatchesCssReferenceValues() {
        val curve = CameraAnimation.EASE_IN_OUT
        assertEquals(0.0, curve.solve(0.0), eps)
        assertEquals(1.0, curve.solve(1.0), eps)
        assertEquals(0.5, curve.solve(0.5), 1e-4)
        // Symmetric around the midpoint and slow at both ends.
        assertEquals(1.0 - curve.solve(0.25), curve.solve(0.75), 1e-4)
        assertTrue(curve.solve(0.1) < 0.1)
        assertTrue(curve.solve(0.9) > 0.9)
        // Reference value for cubic-bezier(0.42, 0, 0.58, 1) at x = 0.25.
        assertEquals(0.1291, curve.solve(0.25), 1e-3)
    }

    @Test
    fun startAndEndAreExact() {
        val from = CameraPosition(coord(10.0, 20.0), 3.0, 30.0, 10.0)
        val to = CameraPosition(coord(-5.0, 40.0), 9.0, 90.0, 45.0)
        val anim = CameraAnimation(from, to, 1000)
        assertEquals(from, anim.valueAt(0))
        assertEquals(to, anim.valueAt(1000))
        assertEquals(to, anim.valueAt(5000))
        assertFalse(anim.isFinished(999))
        assertTrue(anim.isFinished(1000))
    }

    @Test
    fun zeroDurationJumpsToTarget() {
        val to = CameraPosition(coord(1.0, 1.0), 5.0)
        val anim = CameraAnimation(CameraPosition(coord(0.0, 0.0), 1.0), to, 0)
        assertEquals(to, anim.valueAt(0))
        assertTrue(anim.isFinished(0))
    }

    @Test
    fun midpointInterpolatesZoomTiltAndCenter() {
        val from = CameraPosition(coord(0.0, -10.0), 2.0, 0.0, 0.0)
        val to = CameraPosition(coord(0.0, 10.0), 4.0, 0.0, 40.0)
        val mid = CameraAnimation(from, to, 1000).valueAt(500)
        assertEquals(3.0, mid.zoom, 1e-3)
        assertEquals(20.0, mid.tilt, 1e-3)
        assertEquals(0.0, mid.center.longitude, 1e-3)
        assertEquals(0.0, mid.center.latitude, 1e-9)
    }

    @Test
    fun bearingTakesTheShorterArc() {
        val from = CameraPosition(coord(0.0, 0.0), 1.0, bearing = 350.0)
        val to = CameraPosition(coord(0.0, 0.0), 1.0, bearing = 10.0)
        val mid = CameraAnimation(from, to, 1000).valueAt(500)
        assertEquals(0.0, mid.bearing, 1e-3)
    }

    @Test
    fun centerCrossesTheAntimeridianTheShortWay() {
        val from = CameraPosition(coord(0.0, 170.0), 1.0)
        val to = CameraPosition(coord(0.0, -170.0), 1.0)
        val mid = CameraAnimation(from, to, 1000).valueAt(500)
        assertEquals(180.0, kotlin.math.abs(mid.center.longitude), 1e-3)
    }

    @Test
    fun mercatorRoundTrip() {
        listOf(-80.0, -45.0, 0.0, 33.3, 80.0).forEach { lat ->
            val y = CameraAnimation.mercatorY(lat)
            assertEquals(lat, CameraAnimation.latitudeFromMercatorY(y), 1e-9)
        }
    }
}
