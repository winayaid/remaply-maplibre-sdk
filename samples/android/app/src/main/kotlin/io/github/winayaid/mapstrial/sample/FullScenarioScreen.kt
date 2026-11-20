package io.github.winayaid.mapstrial.sample

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.winayaid.mapstrial.MapStyle
import io.github.winayaid.mapstrial.compose.RemaplyMap
import io.github.winayaid.mapstrial.compose.rememberMapController

/**
 * Sample 4: the Compose example. Runs the reference scenario (API_SPEC.md §16), which the iOS
 * SwiftUI example mirrors: three markers, taps, an animation, and every event.
 */
@Composable
fun FullScenarioScreen(style: MapStyle) {
    val controller = rememberMapController()
    var status by rememberSaveable { mutableStateOf("Loading map…") }
    var camera by rememberSaveable { mutableStateOf("") }

    MapScreen(
        map = {
            RemaplyMap(
                style = style,
                modifier = Modifier.fillMaxSize(),
                controller = controller,
                options = SampleScenario.options,
                onMapReady = {
                    SampleScenario.addMarkersIfNeeded(it)
                    status = "Map ready · ${it.markers.size} markers"
                },
                onMapLoadError = { status = "Load error: ${it.code}" },
                onMapTap = { status = "Map tap: ${SampleScenario.describe(it)}" },
                onMarkerTap = { status = "Marker tap: ${it.id}" },
                onCameraChange = { c, reason -> camera = SampleScenario.describe(c, reason) },
                onCameraIdle = { camera = "Idle · ${SampleScenario.describe(it)}" },
            )
        },
        panel = {
            StatusText(status)
            StatusText(camera, secondary = true)
            ButtonRow {
                SampleButton("Animate") {
                    SampleScenario.animate(controller) { finished -> status = "Animation finished: $finished" }
                }
                SampleButton("Reset") { SampleScenario.reset(controller) }
            }
        },
    )
}
