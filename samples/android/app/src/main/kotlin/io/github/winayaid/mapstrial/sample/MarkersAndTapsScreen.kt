package io.github.winayaid.mapstrial.sample

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.winayaid.mapstrial.CameraPosition
import io.github.winayaid.mapstrial.CameraUpdate
import io.github.winayaid.mapstrial.GeoCoordinate
import io.github.winayaid.mapstrial.MapOptions
import io.github.winayaid.mapstrial.Marker
import io.github.winayaid.mapstrial.MarkerAnchor
import io.github.winayaid.mapstrial.compose.RemaplyMap
import io.github.winayaid.mapstrial.compose.rememberMapController

/**
 * Sample 2: markers and taps.
 * - Tap the map to add a marker where you tapped.
 * - Tap a marker to bring it to the front and center the map on it.
 * - "Clear" removes every marker.
 */
@Composable
fun MarkersAndTapsScreen() {
    val controller = rememberMapController()
    var status by rememberSaveable { mutableStateOf("Loading map…") }
    var nextId by rememberSaveable { mutableIntStateOf(1) }
    var topZ by rememberSaveable { mutableIntStateOf(0) }

    MapScreen(
        map = {
            RemaplyMap(
                style = SampleScenario.style,
                modifier = Modifier.fillMaxSize(),
                controller = controller,
                options = MapOptions(initialCamera = CameraPosition(GeoCoordinate(48.8566, 2.3522), zoom = 12.0)),
                onMapReady = { status = "Tap the map to add a marker." },
                onMapTap = { coordinate ->
                    val id = "m$nextId"
                    nextId++
                    controller.addMarker(Marker(id, coordinate))
                    status = "Added $id at ${SampleScenario.describe(coordinate)} · ${controller.markers.size} markers"
                },
                onMarkerTap = { marker ->
                    topZ++
                    // updateMarker replaces the marker; a higher zIndex draws it on top.
                    controller.updateMarker(marker.copy(zIndex = topZ, anchor = MarkerAnchor.BOTTOM))
                    controller.animateCamera(CameraUpdate(center = marker.coordinate), 400)
                    status = "Tapped ${marker.id} (now on top)"
                },
            )
        },
        panel = {
            StatusText(status)
            ButtonRow {
                SampleButton("Clear") {
                    controller.removeAllMarkers()
                    status = "All markers removed. Tap the map to add one."
                }
            }
        },
    )
}
