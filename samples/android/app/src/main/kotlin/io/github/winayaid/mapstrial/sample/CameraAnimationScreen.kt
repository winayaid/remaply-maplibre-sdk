package io.github.winayaid.mapstrial.sample

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.winayaid.mapstrial.CameraPosition
import io.github.winayaid.mapstrial.CameraUpdate
import io.github.winayaid.mapstrial.GeoCoordinate
import io.github.winayaid.mapstrial.MapController
import io.github.winayaid.mapstrial.MapOptions
import io.github.winayaid.mapstrial.compose.RemaplyMap
import io.github.winayaid.mapstrial.compose.rememberMapController

/**
 * Sample 3: moving the camera.
 * - `animateCamera` eases to a target; touching the map cancels it (completion receives false).
 * - `setCamera` jumps without animation.
 * - Camera events show the reason: PROGRAMMATIC for API calls, USER_GESTURE for gestures.
 */
@Composable
fun CameraAnimationScreen() {
    val controller = rememberMapController()
    var status by rememberSaveable { mutableStateOf("Loading map…") }
    var camera by rememberSaveable { mutableStateOf("") }

    fun fly(
        name: String,
        update: CameraUpdate,
        durationMs: Long = 1500,
    ) {
        status = "Animating to $name…"
        controller.animateCamera(update, durationMs) { finished ->
            status = if (finished) "Arrived at $name" else "Animation to $name cancelled"
        }
    }

    MapScreen(
        map = {
            RemaplyMap(
                style = SampleScenario.style,
                modifier = Modifier.fillMaxSize(),
                controller = controller,
                options = MapOptions(initialCamera = CameraPosition(GeoCoordinate(48.8566, 2.3522), zoom = 4.0)),
                onMapReady = { status = "Pick a destination." },
                onCameraChange = { position, reason -> camera = SampleScenario.describe(position, reason) },
                onCameraIdle = { camera = "Idle · ${SampleScenario.describe(it)}" },
            )
        },
        panel = {
            StatusText(status)
            StatusText(camera, secondary = true)
            ButtonRow {
                SampleButton(
                    "Paris",
                ) { fly("Paris", CameraUpdate(center = PARIS, zoom = 11.0, bearing = 0.0, tilt = 0.0)) }
                SampleButton(
                    "Tokyo",
                ) { fly("Tokyo", CameraUpdate(center = TOKYO, zoom = 11.0, bearing = 0.0, tilt = 0.0)) }
                SampleButton("New York") { fly("New York", CameraUpdate(center = NEW_YORK, zoom = 11.0)) }
                SampleButton("Rotate 90°") { rotate(controller) { status = it } }
                SampleButton("Tilt 45°") { fly("a 45° tilt", CameraUpdate(tilt = 45.0), durationMs = 600) }
                SampleButton("Jump to world") {
                    controller.setCamera(
                        CameraUpdate(center = GeoCoordinate(20.0, 0.0), zoom = 1.0, bearing = 0.0, tilt = 0.0),
                    )
                    status = "Jumped (setCamera, no animation)"
                }
            }
        },
    )
}

private fun rotate(
    controller: MapController,
    onStatus: (String) -> Unit,
) {
    val bearing = (controller.camera?.bearing ?: 0.0) + 90.0
    onStatus("Rotating…")
    controller.animateCamera(CameraUpdate(bearing = bearing), 800) { finished ->
        onStatus(if (finished) "Rotated to ${bearing.toInt() % 360}°" else "Rotation cancelled")
    }
}

private val PARIS = GeoCoordinate(48.8566, 2.3522)
private val TOKYO = GeoCoordinate(35.6812, 139.7671)
private val NEW_YORK = GeoCoordinate(40.7128, -74.0060)
