package com.remaply.maps.internal

import com.remaply.maps.MapErrorCode
import com.remaply.maps.MapStyle
import java.net.URI
import java.net.URISyntaxException

/** Validation rules for [MapStyle] from API_SPEC.md §5.4. Returns a message, or `null` if valid. */
internal object StyleValidation {
    fun problem(style: MapStyle): Pair<MapErrorCode, String>? =
        when (style) {
            is MapStyle.Url -> urlProblem(style.url)?.let { MapErrorCode.INVALID_ARGUMENT to it }
            is MapStyle.Bundled -> bundledProblem(style.path)?.let { MapErrorCode.INVALID_ARGUMENT to it }
        }

    private fun urlProblem(url: String): String? {
        val uri =
            try {
                URI(url.trim())
            } catch (e: URISyntaxException) {
                return "Style URL is not a valid URL: '$url' (${e.reason})"
            }
        val scheme = uri.scheme?.lowercase()
        return when {
            !uri.isAbsolute -> "Style URL must be absolute: '$url'"
            scheme != "http" && scheme != "https" -> "Style URL must use http or https: '$url'"
            uri.host.isNullOrEmpty() -> "Style URL has no host: '$url'"
            else -> null
        }
    }

    private fun bundledProblem(path: String): String? =
        when {
            path.isBlank() -> "Bundled style path is empty"
            path.split('/', '\\').any { it == ".." } -> "Bundled style path must not contain '..': '$path'"
            else -> null
        }
}
