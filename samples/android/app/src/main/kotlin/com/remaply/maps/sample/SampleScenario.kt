package com.remaply.maps.sample

import com.remaply.maps.CameraChangeReason
import com.remaply.maps.CameraPosition
import com.remaply.maps.CameraUpdate
import com.remaply.maps.GeoCoordinate
import com.remaply.maps.MapController
import com.remaply.maps.MapOptions
import com.remaply.maps.MapStyle
import com.remaply.maps.Marker
import java.util.Locale

/**
 * The reference parity scenario (API_SPEC.md §16), shared by the Compose and View samples
 * and mirrored by the iOS sample.
 */
object SampleScenario {
    /** Free, keyless demo style. The SDK never hardcodes a style. */
    val style: MapStyle = MapStyle.url("https://tiles.openfreemap.org/styles/liberty")

    val options: MapOptions =
        MapOptions(initialCamera = CameraPosition(GeoCoordinate(48.8566, 2.3522), zoom = 12.0))

    val markers: List<Marker> =
        listOf(
            Marker("a", GeoCoordinate(48.8584, 2.2945)),
            Marker("b", GeoCoordinate(48.8606, 2.3376), zIndex = 1),
            Marker("c", GeoCoordinate(48.8530, 2.3499)),
        )

    /** Adds the markers once. Retained controllers already have them after rotation. */
    fun addMarkersIfNeeded(controller: MapController) {
        if (controller.markers.isEmpty()) markers.forEach(controller::addMarker)
    }

    fun animate(
        controller: MapController,
        onDone: (Boolean) -> Unit,
    ) = controller.animateCamera(CameraUpdate(zoom = 13.0, bearing = 45.0), durationMs = 1000, completion = onDone)

    fun reset(controller: MapController) =
        controller.setCamera(
            CameraUpdate(center = GeoCoordinate(48.8566, 2.3522), zoom = 12.0, bearing = 0.0, tilt = 0.0),
        )

    fun describe(coordinate: GeoCoordinate): String =
        String.format(Locale.US, "%.5f, %.5f", coordinate.latitude, coordinate.longitude)

    fun describe(
        camera: CameraPosition,
        reason: CameraChangeReason? = null,
    ): String {
        val base =
            String.format(
                Locale.US,
                "zoom %.2f · bearing %.0f° · tilt %.0f°",
                camera.zoom,
                camera.bearing,
                camera.tilt,
            )
        return if (reason == null) base else "$base ($reason)"
    }
}
