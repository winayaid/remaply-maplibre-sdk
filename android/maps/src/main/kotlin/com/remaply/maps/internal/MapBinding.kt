package com.remaply.maps.internal

import com.remaply.maps.CameraPosition
import com.remaply.maps.MapStyle
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

/**
 * What a [com.remaply.maps.MapController] needs from the view it is bound to.
 *
 * The controller only talks to this interface, which keeps it free of Views and Contexts and
 * makes its logic testable on the JVM with a fake binding.
 */
internal interface MapBinding {
    /** `true` from just before `onMapReady` until the view is destroyed or unbound. */
    val isReady: Boolean

    val minZoom: Double
    val maxZoom: Double

    /** The live camera. Only meaningful while [isReady]. */
    fun currentCamera(): CameraPosition

    /** Loads [style] unless it is already loaded or loading. */
    fun requestStyle(style: MapStyle)

    /** Moves the camera without animation, emitting change + idle if it moved. */
    fun jumpTo(target: CameraPosition)

    /** Animates to [target]; calls [completion] exactly once. */
    fun animateTo(
        target: CameraPosition,
        durationMs: Long,
        completion: ((Boolean) -> Unit)?,
    )

    /** Cancels a running animation, if any (its completion receives `false`). */
    fun cancelAnimation()

    /** The controller's markers changed; re-render on the next frame. */
    fun markersChanged()

    /** Another view bound the same controller (API_SPEC.md §6.6). */
    fun onUnboundByController()

    val nativeMap: MapLibreMap?
    val nativeView: MapView?
}
