package io.github.winayaid.mapstrial.sample

import io.github.winayaid.mapstrial.CameraChangeReason
import io.github.winayaid.mapstrial.CameraPosition
import io.github.winayaid.mapstrial.CameraUpdate
import io.github.winayaid.mapstrial.GeoCoordinate
import io.github.winayaid.mapstrial.MapController
import io.github.winayaid.mapstrial.MapOptions
import io.github.winayaid.mapstrial.MapStyle
import io.github.winayaid.mapstrial.Marker
import java.util.Locale

/**
 * The reference parity scenario (API_SPEC.md §16), shared by the Compose and View samples
 * and mirrored by the iOS sample.
 */
object SampleScenario {
    /** Free, keyless demo style. The SDK never hardcodes a style. */
    private val DEFAULT_STYLE: MapStyle = MapStyle.url("https://tiles.openfreemap.org/styles/liberty")

    /**
     * The style the sample screens use. The launcher intent extra `sampleStyleAsset` replaces it with
     * a bundled style without leaving the launcher (used by the sample-screen UI tests).
     */
    var style: MapStyle = DEFAULT_STYLE

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
