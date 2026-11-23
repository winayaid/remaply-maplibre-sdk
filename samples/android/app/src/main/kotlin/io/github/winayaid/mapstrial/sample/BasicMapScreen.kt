package io.github.winayaid.mapstrial.sample

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.winayaid.mapstrial.CameraPosition
import io.github.winayaid.mapstrial.GeoCoordinate
import io.github.winayaid.mapstrial.MapOptions
import io.github.winayaid.mapstrial.compose.RemaplyMap

/**
 * Sample 1: the smallest useful map. One style, one starting camera, and error reporting.
 * Everything else (gestures, lifecycle, rotation) is handled by the SDK.
 */
@Composable
fun BasicMapScreen() {
    var status by rememberSaveable { mutableStateOf("Loading…") }
    MapScreen(
        map = {
            RemaplyMap(
                style = SampleScenario.style,
                modifier = Modifier.fillMaxSize(),
                options = MapOptions(initialCamera = CameraPosition(GeoCoordinate(48.8566, 2.3522), zoom = 11.0)),
                onMapReady = { status = "Map ready. Pan, pinch, rotate, and tilt with your fingers." },
                onMapLoadError = { status = "Could not load the style: ${it.code}" },
            )
        },
        panel = { StatusText(status) },
    )
}
