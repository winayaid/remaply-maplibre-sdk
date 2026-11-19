package com.remaply.maps.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.remaply.maps.CameraChangeReason
import com.remaply.maps.CameraPosition
import com.remaply.maps.GeoCoordinate
import com.remaply.maps.MapController
import com.remaply.maps.MapError
import com.remaply.maps.MapListener
import com.remaply.maps.MapOptions
import com.remaply.maps.MapStyle
import com.remaply.maps.Marker
import com.remaply.maps.RemaplyMapView

/**
 * A MapLibre map for Jetpack Compose.
 *
 * ```
 * val controller = rememberMapController()
 * RemaplyMap(
 *     style = MapStyle.url("https://example.com/style.json"),
 *     modifier = Modifier.fillMaxSize(),
 *     controller = controller,
 *     onMarkerTap = { marker -> /* ... */ },
 * )
 * ```
 *
 * [style] is the source of truth: changing it loads the new style. [options] changes apply
 * immediately, except [MapOptions.initialCamera]. The latest callback lambdas are always used.
 * Lifecycle is handled automatically; leaving the composition destroys the native map.
 *
 * Spec: API-031.
 *
 * @param style The style to show.
 * @param modifier Layout modifier.
 * @param controller The map's controller. The default is retained across configuration changes.
 * @param options Map options.
 * @param onMapReady The first style loaded on this view and the map is ready.
 * @param onMapLoadError A style request failed, or the renderer could not start.
 * @param onStyleLoaded A style finished loading, including the first.
 * @param onMapTap A single tap that did not hit a marker.
 * @param onMarkerTap A single tap on a marker (the topmost one).
 * @param onCameraChange The camera changed (at most once per frame).
 * @param onCameraIdle The camera stopped moving.
 */
@Composable
public fun RemaplyMap(
    style: MapStyle,
    modifier: Modifier = Modifier,
    controller: MapController = rememberMapController(),
    options: MapOptions = MapOptions(),
    onMapReady: (MapController) -> Unit = {},
    onMapLoadError: (MapError) -> Unit = {},
    onStyleLoaded: (MapStyle) -> Unit = {},
    onMapTap: (GeoCoordinate) -> Unit = {},
    onMarkerTap: (Marker) -> Unit = {},
    onCameraChange: (CameraPosition, CameraChangeReason) -> Unit = { _, _ -> },
    onCameraIdle: (CameraPosition) -> Unit = {},
) {
    val currentOnMapReady by rememberUpdatedState(onMapReady)
    val currentOnMapLoadError by rememberUpdatedState(onMapLoadError)
    val currentOnStyleLoaded by rememberUpdatedState(onStyleLoaded)
    val currentOnMapTap by rememberUpdatedState(onMapTap)
    val currentOnMarkerTap by rememberUpdatedState(onMarkerTap)
    val currentOnCameraChange by rememberUpdatedState(onCameraChange)
    val currentOnCameraIdle by rememberUpdatedState(onCameraIdle)

    val listener =
        remember {
            object : MapListener {
                override fun onMapReady(controller: MapController) = currentOnMapReady(controller)

                override fun onMapLoadError(error: MapError) = currentOnMapLoadError(error)

                override fun onStyleLoaded(style: MapStyle) = currentOnStyleLoaded(style)

                override fun onMapTap(coordinate: GeoCoordinate) = currentOnMapTap(coordinate)

                override fun onMarkerTap(marker: Marker) = currentOnMarkerTap(marker)

                override fun onCameraChange(
                    camera: CameraPosition,
                    reason: CameraChangeReason,
                ) = currentOnCameraChange(camera, reason)

                override fun onCameraIdle(camera: CameraPosition) = currentOnCameraIdle(camera)
            }
        }

    // A different controller instance needs a new native view bound to it.
    key(controller) {
        AndroidView(
            factory = { context ->
                RemaplyMapView(context, style, options, controller).also { it.listener = listener }
            },
            modifier = modifier,
            update = { view ->
                if (view.options != options) view.options = options
                if (controller.style != style) controller.setStyle(style)
            },
            onRelease = { view -> view.onDestroy() },
        )
    }
}

/**
 * Remembers a [MapController] that survives recomposition and configuration changes such as
 * rotation. It is kept in a `ViewModel` of the current `ViewModelStoreOwner`.
 *
 * Spec: API-032.
 *
 * @param key Distinguishes several maps on one screen. When `null`, the call site's position in
 *   the composition is used.
 */
@Composable
public fun rememberMapController(key: String? = null): MapController {
    val owner = LocalViewModelStoreOwner.current ?: return remember(key) { MapController() }
    val resolvedKey = key ?: currentCompositeKeyHashCode.toString(KEY_RADIX)
    val holder: MapControllerHolder =
        viewModel(viewModelStoreOwner = owner, key = "remaply-map-controller:$resolvedKey")
    return holder.controller
}

/** Keeps a controller alive across configuration changes. */
internal class MapControllerHolder : ViewModel() {
    val controller: MapController = MapController()
}

private const val KEY_RADIX = 36
