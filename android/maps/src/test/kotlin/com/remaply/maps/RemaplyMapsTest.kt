package com.remaply.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemaplyMapsTest {
    private val semVer = Regex("""^\d+\.\d+\.\d+(-[0-9A-Za-z.-]+)?$""")

    @Test
    fun version_isSemVer() {
        assertTrue("version '${RemaplyMaps.version}' is not SemVer", semVer.matches(RemaplyMaps.version))
    }

    @Test
    fun mapLibreVersion_isPinnedRelease() {
        // Pinned per API_SPEC.md §1. A prerelease or "-globe" build must never be used.
        assertEquals("13.7.1", RemaplyMaps.mapLibreVersion)
    }
}
