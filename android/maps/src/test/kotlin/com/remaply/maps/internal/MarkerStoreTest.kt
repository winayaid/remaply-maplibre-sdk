package com.remaply.maps.internal

import com.remaply.maps.MapErrorCode
import com.remaply.maps.Marker
import com.remaply.maps.assertMapError
import com.remaply.maps.coord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkerStoreTest {
    private fun marker(
        id: String,
        z: Int = 0,
        lat: Double = 0.0,
    ) = Marker(id, coord(lat, 0.0), zIndex = z)

    @Test
    fun addGetAndInsertionOrder() {
        val store = MarkerStore()
        store.add(marker("a"))
        store.add(marker("b"))
        store.add(marker("c"))
        assertEquals(listOf("a", "b", "c"), store.all.map { it.id })
        assertEquals("b", store["b"]?.id)
        assertNull(store["zzz"])
    }

    @Test
    fun addRejectsEmptyIdInvalidCoordinateAndDuplicates() {
        val store = MarkerStore()
        assertMapError(MapErrorCode.INVALID_ARGUMENT) { store.add(marker("")) }
        assertMapError(MapErrorCode.INVALID_ARGUMENT) { store.add(Marker("x", coord(100.0, 0.0))) }
        store.add(marker("a"))
        assertMapError(MapErrorCode.DUPLICATE_MARKER_ID) { store.add(marker("a", lat = 5.0)) }
        // A failed call changes nothing.
        assertEquals(listOf(marker("a")), store.all)
    }

    @Test
    fun validationOrderIsIdThenCoordinateThenExistence() {
        val store = MarkerStore()
        store.add(marker("a"))
        // Duplicate id with invalid coordinate: the coordinate error wins.
        assertMapError(MapErrorCode.INVALID_ARGUMENT) { store.add(Marker("a", coord(100.0, 0.0))) }
        // Unknown id with invalid coordinate: the coordinate error wins.
        assertMapError(MapErrorCode.INVALID_ARGUMENT) { store.update(Marker("zzz", coord(100.0, 0.0))) }
    }

    @Test
    fun updateReplacesKeepsPositionAndRejectsUnknown() {
        val store = MarkerStore()
        store.add(marker("a"))
        store.add(marker("b"))
        store.update(marker("a", lat = 10.0))
        assertEquals(listOf("a", "b"), store.all.map { it.id })
        assertEquals(10.0, store["a"]!!.coordinate.latitude, 0.0)
        assertMapError(MapErrorCode.MARKER_NOT_FOUND) { store.update(marker("zzz")) }
        assertMapError(MapErrorCode.INVALID_ARGUMENT) { store.update(marker("")) }
    }

    @Test
    fun removeAndClear() {
        val store = MarkerStore()
        store.add(marker("a"))
        assertTrue(store.remove("a"))
        assertFalse(store.remove("a"))
        assertFalse(store.remove("zzz"))
        store.add(marker("b"))
        store.clear()
        assertTrue(store.isEmpty)
    }

    @Test
    fun drawOrderIsZIndexThenRecency() {
        val store = MarkerStore()
        store.add(marker("a"))
        store.add(marker("b", z = 1))
        store.add(marker("c"))
        assertEquals(listOf("a", "c", "b"), store.drawOrder().map { it.id })
        // Updating "a" makes it the most recent among zIndex 0.
        store.update(marker("a"))
        assertEquals(listOf("c", "a", "b"), store.drawOrder().map { it.id })
    }

    @Test
    fun topmostAmongHits() {
        val store = MarkerStore()
        store.add(marker("a"))
        store.add(marker("b", z = 1))
        store.add(marker("c"))
        assertEquals("b", store.topmost(listOf("a", "b", "c"))?.id)
        assertEquals("c", store.topmost(listOf("a", "c"))?.id)
        assertEquals("a", store.topmost(listOf("a", "unknown"))?.id)
        assertNull(store.topmost(emptyList()))
    }
}
