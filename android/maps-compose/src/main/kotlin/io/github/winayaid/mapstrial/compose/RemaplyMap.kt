package io.github.winayaid.mapstrial.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.winayaid.mapstrial.CameraChangeReason
import io.github.winayaid.mapstrial.CameraPosition
import io.github.winayaid.mapstrial.CameraUpdate
import io.github.winayaid.mapstrial.GeoCoordinate
import io.github.winayaid.mapstrial.MapController
import io.github.winayaid.mapstrial.MapError
import io.github.winayaid.mapstrial.MapListener
import io.github.winayaid.mapstrial.MapOptions
import io.github.winayaid.mapstrial.MapStyle
import io.github.winayaid.mapstrial.Marker
import io.github.winayaid.mapstrial.RemaplyMapView

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
 * rotation. It is kept in a `ViewModel` of the current `ViewModelStoreOwner`. Its style and camera
 * are also saved with the screen state, so they come back after process death (markers do not).
 *
 * Spec: API-032.
 *
 * @param key Distinguishes several maps on one screen. When `null`, the call site's position in
 *   the composition is used.
 */
@Composable
public fun rememberMapController(key: String? = null): MapController {
    val owner = LocalViewModelStoreOwner.current
    val resolvedKey = key ?: currentCompositeKeyHashCode.toString(KEY_RADIX)
    val controller =
        if (owner == null) {
            remember(key) { MapController() }
        } else {
            val holder: MapControllerHolder =
                viewModel(viewModelStoreOwner = owner, key = "remaply-map-controller:$resolvedKey")
            holder.controller
        }
    // Process death (API_SPEC.md §9.3): style and camera are saved with the screen's state and restored
    // into a fresh controller. After a configuration change the retained controller already has them,
    // so nothing is overwritten. Markers are not saved; re-add them in onMapReady.
    rememberSaveable(key, saver = savedStateSaver(controller)) {}
    return controller
}

private fun savedStateSaver(controller: MapController): Saver<Unit, List<Any>> =
    Saver(
        save = { snapshot(controller) },
        restore = { saved ->
            restoreInto(controller, saved)
            Unit
        },
    )

private fun snapshot(controller: MapController): List<Any> {
    val camera = controller.camera
    val style = controller.style
    val cameraFields =
        camera?.let { doubleArrayOf(it.center.latitude, it.center.longitude, it.zoom, it.bearing, it.tilt) }
            ?: DoubleArray(0)
    val (kind, value) =
        when (style) {
            is MapStyle.Url -> "url" to style.url
            is MapStyle.Bundled -> "bundled" to style.path
            null -> "" to ""
        }
    return listOf(cameraFields, kind, value)
}

private fun restoreInto(
    controller: MapController,
    saved: List<Any>,
) {
    val camera = saved.getOrNull(0) as? DoubleArray
    val kind = saved.getOrNull(1) as? String
    val value = saved.getOrNull(2) as? String
    if (controller.style == null && value != null) {
        when (kind) {
            "url" -> controller.setStyle(MapStyle.url(value))
            "bundled" -> controller.setStyle(MapStyle.bundled(value))
        }
    }
    if (controller.camera == null && camera != null && camera.size == CAMERA_FIELDS) {
        controller.setCamera(CameraUpdate(GeoCoordinate(camera[0], camera[1]), camera[2], camera[3], camera[4]))
    }
}

/** Keeps a controller alive across configuration changes. */
internal class MapControllerHolder : ViewModel() {
    val controller: MapController = MapController()
}

private const val KEY_RADIX = 36
private const val CAMERA_FIELDS = 5
