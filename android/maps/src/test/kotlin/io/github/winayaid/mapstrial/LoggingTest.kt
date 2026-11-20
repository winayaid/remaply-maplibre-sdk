package io.github.winayaid.mapstrial

import io.github.winayaid.mapstrial.internal.SdkLog
import io.github.winayaid.mapstrial.internal.mapError
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoggingTest {
    private val entries = mutableListOf<Triple<LogLevel, String, String>>()

    @After
    fun tearDown() {
        RemaplyMaps.logger = null
        RemaplyMaps.logLevel = LogLevel.WARN
    }

    private fun capture() {
        RemaplyMaps.logger = MapLogger { level, tag, message, _ -> entries += Triple(level, tag, message) }
    }

    @Test
    fun defaultLevelIsWarn() {
        assertEquals(LogLevel.WARN, RemaplyMaps.logLevel)
    }

    @Test
    fun messagesBelowTheLevelAreDropped() {
        capture()
        RemaplyMaps.logLevel = LogLevel.WARN
        SdkLog.debug("d")
        SdkLog.info("i")
        SdkLog.warn("w")
        SdkLog.error("e")
        assertEquals(listOf(LogLevel.WARN, LogLevel.ERROR), entries.map { it.first })
        assertTrue(entries.all { it.second == "Remaply" })
    }

    @Test
    fun noneDisablesEverything() {
        capture()
        RemaplyMaps.logLevel = LogLevel.NONE
        SdkLog.error("e")
        assertTrue(entries.isEmpty())
    }

    @Test
    fun everySdkErrorIsReported() {
        capture()
        mapError(MapErrorCode.MARKER_NOT_FOUND, "nope")
        assertEquals(LogLevel.WARN, entries.single().first)
        assertTrue(entries.single().third.contains("MARKER_NOT_FOUND"))
    }

    @Test
    fun aThrowingLoggerDoesNotBreakTheSdk() {
        RemaplyMaps.logger = MapLogger { _, _, _, _ -> error("logger bug") }
        SdkLog.error("still fine")
    }

    @Test
    fun errorCodeIsExposedOnMapError() {
        val error = MapError(MapErrorCode.STYLE_NOT_FOUND, "missing")
        assertEquals(MapErrorCode.STYLE_NOT_FOUND, error.code)
        assertEquals("missing", error.message)
    }
}
