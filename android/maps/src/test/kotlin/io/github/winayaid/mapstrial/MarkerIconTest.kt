package io.github.winayaid.mapstrial

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class MarkerIconTest {
    @Test
    fun defaultPinIsASingleEqualValue() {
        assertSame(MarkerIcon.defaultPin(), MarkerIcon.defaultPin())
        assertEquals(MarkerIcon.defaultPin(), Marker("a", coord(0.0, 0.0)).icon)
    }

    @Test
    fun markerDefaults() {
        val m = Marker("a", coord(1.0, 2.0))
        assertEquals(MarkerAnchor.BOTTOM, m.anchor)
        assertEquals(0, m.zIndex)
    }

    @Test
    fun optionDefaultsMatchSpec() {
        val o = MapOptions()
        assertEquals(null, o.initialCamera)
        assertEquals(0.0, o.minZoom, 0.0)
        assertEquals(22.0, o.maxZoom, 0.0)
        assertEquals(
            listOf(true, true, true, true, true),
            listOf(
                o.scrollGesturesEnabled,
                o.zoomGesturesEnabled,
                o.rotateGesturesEnabled,
                o.tiltGesturesEnabled,
                o.compassEnabled,
            ),
        )
    }
}
