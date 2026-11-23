package io.github.winayaid.mapstrial.kmp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import io.github.winayaid.mapstrial.CameraPosition
import io.github.winayaid.mapstrial.CameraUpdate
import io.github.winayaid.mapstrial.GeoCoordinate
import io.github.winayaid.mapstrial.MapOptions
import io.github.winayaid.mapstrial.MapStyle
import io.github.winayaid.mapstrial.Marker
import io.github.winayaid.mapstrial.compose.RemaplyMap
import io.github.winayaid.mapstrial.compose.rememberMapController

@Composable
actual fun PlatformMap(
    places: List<Place>,
    cameraTarget: CameraTarget,
    onMapReady: () -> Unit,
    onPlaceTap: (placeId: String) -> Unit,
    onMapTap: (latitude: Double, longitude: Double) -> Unit,
    modifier: Modifier,
) {
    val controller = rememberMapController()
    val currentPlaces by rememberUpdatedState(places)

    RemaplyMap(
        style = MapStyle.url(MAP_STYLE_URL),
        modifier = modifier,
        controller = controller,
        options = MapOptions(initialCamera = CameraPosition(cameraTarget.toCoordinate(), cameraTarget.zoom)),
        onMapReady = { map ->
            // The controller survives rotation with its markers, so only add the missing ones.
            currentPlaces.filter { map.getMarker(it.id) == null }.forEach {
                map.addMarker(Marker(it.id, GeoCoordinate(it.latitude, it.longitude)))
            }
            onMapReady()
        },
        onMarkerTap = { onPlaceTap(it.id) },
        onMapTap = { onMapTap(it.latitude, it.longitude) },
    )

    LaunchedEffect(cameraTarget.requestId) {
        if (cameraTarget.requestId > 0 && controller.isMapReady) {
            controller.animateCamera(CameraUpdate(center = cameraTarget.toCoordinate(), zoom = cameraTarget.zoom), FLY_DURATION_MS)
        }
    }
}

private fun CameraTarget.toCoordinate() = GeoCoordinate(latitude, longitude)

private const val FLY_DURATION_MS = 1500L
