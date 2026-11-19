package com.remaply.maps.internal

import com.remaply.maps.CameraChangeReason.PROGRAMMATIC
import com.remaply.maps.CameraChangeReason.USER_GESTURE
import com.remaply.maps.CameraPosition
import com.remaply.maps.coord
import org.junit.Assert.assertEquals
import org.junit.Test

class CameraEventGateTest {
    private val events = mutableListOf<String>()
    private val gate =
        CameraEventGate(
            emitChange = { c, r -> events += "change(${c.zoom},$r)" },
            emitIdle = { c -> events += "idle(${c.zoom})" },
        )

    private fun cam(zoom: Double) = CameraPosition(coord(0.0, 0.0), zoom)

    @Test
    fun nothingIsEmittedBeforeEnable() {
        gate.change(cam(2.0), USER_GESTURE)
        gate.idle(cam(2.0), USER_GESTURE)
        assertEquals(emptyList<String>(), events)
    }

    @Test
    fun movementIsChangesThenOneIdle() {
        gate.enable(cam(1.0))
        gate.change(cam(2.0), USER_GESTURE)
        gate.change(cam(3.0), USER_GESTURE)
        gate.idle(cam(3.0), USER_GESTURE)
        assertEquals(listOf("change(2.0,USER_GESTURE)", "change(3.0,USER_GESTURE)", "idle(3.0)"), events)
    }

    @Test
    fun duplicatePositionsAreDropped() {
        gate.enable(cam(1.0))
        gate.change(cam(1.0), USER_GESTURE)
        gate.change(cam(2.0), USER_GESTURE)
        gate.change(cam(2.0), USER_GESTURE)
        gate.idle(cam(2.0), USER_GESTURE)
        gate.idle(cam(2.0), USER_GESTURE) // stray second idle
        assertEquals(listOf("change(2.0,USER_GESTURE)", "idle(2.0)"), events)
    }

    @Test
    fun jumpEmitsExactlyOneChangeAndIdle() {
        gate.enable(cam(1.0))
        gate.idle(cam(5.0), PROGRAMMATIC)
        assertEquals(listOf("change(5.0,PROGRAMMATIC)", "idle(5.0)"), events)
    }

    @Test
    fun jumpToSamePositionEmitsNothing() {
        gate.enable(cam(1.0))
        gate.idle(cam(1.0), PROGRAMMATIC)
        assertEquals(emptyList<String>(), events)
    }

    @Test
    fun disableStopsEvents() {
        gate.enable(cam(1.0))
        gate.disable()
        gate.idle(cam(4.0), PROGRAMMATIC)
        assertEquals(emptyList<String>(), events)
    }
}
