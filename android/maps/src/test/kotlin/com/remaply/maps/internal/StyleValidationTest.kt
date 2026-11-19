package com.remaply.maps.internal

import com.remaply.maps.MapErrorCode
import com.remaply.maps.MapStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StyleValidationTest {
    @Test
    fun acceptsAbsoluteHttpAndHttpsUrls() {
        assertNull(StyleValidation.problem(MapStyle.url("https://example.com/style.json")))
        assertNull(StyleValidation.problem(MapStyle.url("http://example.com/style.json?key=abc")))
        assertNull(StyleValidation.problem(MapStyle.url("HTTPS://EXAMPLE.COM/style.json")))
    }

    @Test
    fun rejectsOtherUrls() {
        listOf(
            "",
            "style.json",
            "/styles/style.json",
            "ftp://example.com/style.json",
            "file:///sdcard/style.json",
            "asset://style.json",
            "https://",
            "https://exa mple.com/style.json",
        ).forEach { url ->
            assertEquals(url, MapErrorCode.INVALID_ARGUMENT, StyleValidation.problem(MapStyle.url(url))?.first)
        }
    }

    @Test
    fun bundledPathRules() {
        assertNull(StyleValidation.problem(MapStyle.bundled("styles/basic.json")))
        assertNull(StyleValidation.problem(MapStyle.bundled("basic.json")))
        listOf("", "   ", "../secret.json", "styles/../../x.json", "styles\\..\\x.json").forEach { path ->
            assertEquals(path, MapErrorCode.INVALID_ARGUMENT, StyleValidation.problem(MapStyle.bundled(path))?.first)
        }
    }

    @Test
    fun factoriesCreateExpectedKinds() {
        assertEquals(MapStyle.Url("u"), MapStyle.url("u"))
        assertEquals(MapStyle.Bundled("p"), MapStyle.bundled("p"))
    }
}
