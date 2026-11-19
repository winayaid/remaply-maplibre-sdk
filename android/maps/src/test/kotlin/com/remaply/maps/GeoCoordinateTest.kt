package com.remaply.maps

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoCoordinateTest {
    @Test
    fun validRangesAreInclusive() {
        assertTrue(coord(0.0, 0.0).isValid)
        assertTrue(coord(90.0, 180.0).isValid)
        assertTrue(coord(-90.0, -180.0).isValid)
    }

    @Test
    fun outOfRangeOrNonFiniteIsInvalid() {
        assertFalse(coord(90.0001, 0.0).isValid)
        assertFalse(coord(-90.0001, 0.0).isValid)
        assertFalse(coord(0.0, 180.0001).isValid)
        assertFalse(coord(0.0, -180.0001).isValid)
        assertFalse(coord(Double.NaN, 0.0).isValid)
        assertFalse(coord(0.0, Double.POSITIVE_INFINITY).isValid)
    }
}
