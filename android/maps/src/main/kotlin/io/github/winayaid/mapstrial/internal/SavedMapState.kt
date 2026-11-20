package io.github.winayaid.mapstrial.internal

import android.os.Bundle
import io.github.winayaid.mapstrial.CameraPosition
import io.github.winayaid.mapstrial.GeoCoordinate
import io.github.winayaid.mapstrial.MapStyle

/**
 * Style and camera saved across process death (API_SPEC.md §9.3). Markers are not saved because
 * they may hold Bitmaps; apps re-add them in `onMapReady`.
 */
internal object SavedMapState {
    private const val KEY_CAMERA = "remaply.camera"
    private const val KEY_STYLE_KIND = "remaply.style.kind"
    private const val KEY_STYLE_VALUE = "remaply.style.value"
    private const val KIND_URL = "url"
    private const val KIND_BUNDLED = "bundled"

    fun write(
        out: Bundle,
        style: MapStyle?,
        camera: CameraPosition?,
    ) {
        camera?.let {
            out.putDoubleArray(
                KEY_CAMERA,
                doubleArrayOf(it.center.latitude, it.center.longitude, it.zoom, it.bearing, it.tilt),
            )
        }
        when (style) {
            is MapStyle.Url -> {
                out.putString(KEY_STYLE_KIND, KIND_URL)
                out.putString(KEY_STYLE_VALUE, style.url)
            }
            is MapStyle.Bundled -> {
                out.putString(KEY_STYLE_KIND, KIND_BUNDLED)
                out.putString(KEY_STYLE_VALUE, style.path)
            }
            null -> Unit
        }
    }

    fun readCamera(state: Bundle): CameraPosition? {
        val a = state.getDoubleArray(KEY_CAMERA)?.takeIf { it.size == 5 } ?: return null
        return CameraPosition(GeoCoordinate(a[0], a[1]), a[2], a[3], a[4])
    }

    fun readStyle(state: Bundle): MapStyle? {
        val value = state.getString(KEY_STYLE_VALUE) ?: return null
        return when (state.getString(KEY_STYLE_KIND)) {
            KIND_URL -> MapStyle.url(value)
            KIND_BUNDLED -> MapStyle.bundled(value)
            else -> null
        }
    }
}
