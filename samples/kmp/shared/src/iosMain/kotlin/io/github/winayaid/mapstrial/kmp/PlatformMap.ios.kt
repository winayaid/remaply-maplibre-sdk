package io.github.winayaid.mapstrial.kmp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView

internal val LocalNativeMapFactory =
    staticCompositionLocalOf<NativeMapFactory> {
        error("No NativeMapFactory: create the UI with MainViewController(mapFactory)")
    }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun PlatformMap(
    places: List<Place>,
    cameraTarget: CameraTarget,
    onMapReady: () -> Unit,
    onPlaceTap: (placeId: String) -> Unit,
    onMapTap: (latitude: Double, longitude: Double) -> Unit,
    modifier: Modifier,
) {
    val factory = LocalNativeMapFactory.current
    val currentOnMapReady by rememberUpdatedState(onMapReady)
    val currentOnPlaceTap by rememberUpdatedState(onPlaceTap)
    val currentOnMapTap by rememberUpdatedState(onMapTap)

    val nativeMap =
        remember(factory) {
            val listener =
                object : NativeMapListener {
                    override fun onMapReady() = currentOnMapReady()

                    override fun onPlaceTap(placeId: String) = currentOnPlaceTap(placeId)

                    override fun onMapTap(
                        latitude: Double,
                        longitude: Double,
                    ) = currentOnMapTap(latitude, longitude)
                }
            factory.createMap(MAP_STYLE_URL, cameraTarget, places, listener)
        }

    LaunchedEffect(nativeMap, cameraTarget.requestId) {
        if (cameraTarget.requestId > 0) nativeMap.flyTo(cameraTarget)
    }

    UIKitView(
        factory = { nativeMap.view },
        modifier = modifier,
        // The map handles its own pan, pinch, and rotate gestures without waiting for Compose.
        properties =
            UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = true,
            ),
    )
}
