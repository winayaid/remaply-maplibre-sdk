package com.remaply.maps

import com.remaply.maps.internal.CameraMath
import com.remaply.maps.internal.CameraMath.isEmpty
import com.remaply.maps.internal.EventQueue
import com.remaply.maps.internal.MainThread
import com.remaply.maps.internal.MapBinding
import com.remaply.maps.internal.MarkerStore
import com.remaply.maps.internal.SdkLog
import com.remaply.maps.internal.mapError
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

/**
 * The state holder and command interface of a map: style, camera, and markers.
 *
 * A controller never holds a reference to a View or Context while it is not bound, so it is safe
 * to keep in a `ViewModel`. It binds automatically when passed to, or created by, a
 * [RemaplyMapView] or the Compose `RemaplyMap`, and keeps its state when that view is destroyed.
 *
 * All methods must be called on the main thread; other threads get [MapErrorCode.WRONG_THREAD].
 *
 * Spec: API_SPEC.md §6.
 */
public class MapController {
    private val markerStore = MarkerStore()
    private var requestedStyle: MapStyle? = null

    /** Camera kept while no ready map exists: the last live camera, or values set while unbound. */
    internal var storedCamera: CameraPosition? = null

    /** Delivers callbacks in order and without re-entrancy for this controller and its view. */
    internal val events: EventQueue = EventQueue()

    internal var binding: MapBinding? = null
        private set

    /**
     * `true` from just before `onMapReady` until the bound view is destroyed or unbound.
     * Stays `true` while a later style loads. Spec: API-011.
     */
    public val isMapReady: Boolean
        get() = binding?.isReady == true

    /** The most recently requested style, loaded or not. `null` before any request. Spec: API-012. */
    public val style: MapStyle?
        get() = requestedStyle

    /**
     * The live camera while a map is ready, otherwise the stored camera. `null` if no camera was
     * ever set or observed. Spec: API-013.
     */
    public val camera: CameraPosition?
        get() = binding?.takeIf { it.isReady }?.currentCamera() ?: storedCamera

    /** All markers, in insertion order. Spec: API-014. */
    public val markers: List<Marker>
        get() = markerStore.all

    /**
     * Requests a style load. Never throws: failures arrive in [MapListener.onMapLoadError].
     * A request for the style that is already loaded or loading is a no-op. Spec: API-015.
     */
    public fun setStyle(style: MapStyle) {
        MainThread.check("setStyle")
        requestedStyle = style
        binding?.requestStyle(style)
    }

    /**
     * Moves the camera immediately, without animation, cancelling any running animation.
     * Spec: API-016.
     *
     * @throws MapError [MapErrorCode.INVALID_ARGUMENT] if [update] has an invalid value.
     */
    @Throws(MapError::class)
    public fun setCamera(update: CameraUpdate) {
        MainThread.check("setCamera")
        CameraMath.validate(update, "setCamera")
        if (update.isEmpty) return
        val b = binding
        if (b != null && b.isReady) {
            b.jumpTo(CameraMath.apply(b.currentCamera(), update, b.minZoom, b.maxZoom))
        } else {
            storedCamera = applyUnbound(update)
        }
    }

    /**
     * Animates the camera with an ease-in-out curve. Spec: API-017.
     *
     * [completion] is called exactly once: with `true` after the animation finishes (right after
     * `onCameraIdle`), or `false` if it is cancelled by another camera call, a user touch, or the
     * view going away. With no ready map, the stored camera changes immediately and [completion]
     * receives `true`.
     *
     * @param durationMs Duration in milliseconds; 0 jumps.
     * @throws MapError [MapErrorCode.INVALID_ARGUMENT] if [update] has an invalid value or
     *   [durationMs] is negative.
     */
    @JvmOverloads
    @Throws(MapError::class)
    public fun animateCamera(
        update: CameraUpdate,
        durationMs: Long = DEFAULT_ANIMATION_DURATION_MS,
        completion: ((finished: Boolean) -> Unit)? = null,
    ) {
        MainThread.check("animateCamera")
        CameraMath.validate(update, "animateCamera")
        if (durationMs < 0) {
            throw mapError(MapErrorCode.INVALID_ARGUMENT, "animateCamera: durationMs must be >= 0, was $durationMs")
        }
        if (update.isEmpty) {
            completion?.let { events.post { it(true) } }
            return
        }
        val b = binding
        if (b != null && b.isReady) {
            b.animateTo(CameraMath.apply(b.currentCamera(), update, b.minZoom, b.maxZoom), durationMs, completion)
        } else {
            storedCamera = applyUnbound(update)
            completion?.let { events.post { it(true) } }
        }
    }

    /** Returns the marker with [id], or `null`. Spec: API-018. */
    public fun getMarker(id: String): Marker? {
        MainThread.check("getMarker")
        return markerStore[id]
    }

    /**
     * Adds [marker]. It renders on the next frame, or once a map is ready. Spec: API-019.
     *
     * @throws MapError [MapErrorCode.INVALID_ARGUMENT] for an empty id or invalid coordinate;
     *   [MapErrorCode.DUPLICATE_MARKER_ID] if the id already exists.
     */
    @Throws(MapError::class)
    public fun addMarker(marker: Marker) {
        MainThread.check("addMarker")
        markerStore.add(marker)
        binding?.markersChanged()
    }

    /**
     * Replaces the marker with the same id. It then counts as the most recent for draw order.
     * Spec: API-020.
     *
     * @throws MapError [MapErrorCode.INVALID_ARGUMENT] for an empty id or invalid coordinate;
     *   [MapErrorCode.MARKER_NOT_FOUND] if no marker has that id.
     */
    @Throws(MapError::class)
    public fun updateMarker(marker: Marker) {
        MainThread.check("updateMarker")
        markerStore.update(marker)
        binding?.markersChanged()
    }

    /** Removes the marker with [id]. Returns `false` if there was none. Spec: API-021. */
    public fun removeMarker(id: String): Boolean {
        MainThread.check("removeMarker")
        val removed = markerStore.remove(id)
        if (removed) binding?.markersChanged()
        return removed
    }

    /** Removes all markers. Spec: API-022. */
    public fun removeAllMarkers() {
        MainThread.check("removeAllMarkers")
        if (markerStore.isEmpty) return
        markerStore.clear()
        binding?.markersChanged()
    }

    /**
     * Runs [block] synchronously with the native MapLibre objects, only while a map is ready.
     * Returns `false` without running it otherwise. Do not keep references to the objects, and do
     * not remove the SDK's marker layer or replace the style through them.
     *
     * Experimental: not covered by semantic versioning or platform parity. Spec: API-091.
     */
    @ExperimentalRemaplyApi
    public fun withNativeMap(block: (map: MapLibreMap, view: MapView) -> Unit): Boolean {
        MainThread.check("withNativeMap")
        val b = binding?.takeIf { it.isReady } ?: return false
        val map = b.nativeMap ?: return false
        val view = b.nativeView ?: return false
        block(map, view)
        return true
    }

    // ---- Internal: binding (API_SPEC.md §6.6) ----

    internal fun bind(newBinding: MapBinding) {
        val old = binding
        if (old === newBinding) return
        if (old != null) {
            SdkLog.warn(
                "MapController was bound to a new view while another view was still using it; the old view is unbound",
            )
            if (old.isReady) storedCamera = old.currentCamera()
            binding = newBinding
            old.onUnboundByController()
        } else {
            binding = newBinding
        }
    }

    internal fun unbind(oldBinding: MapBinding) {
        if (binding !== oldBinding) return
        if (oldBinding.isReady) storedCamera = oldBinding.currentCamera()
        binding = null
    }

    internal fun markersInDrawOrder(): List<Marker> = markerStore.drawOrder()

    internal fun topmostMarker(ids: Collection<String>): Marker? = markerStore.topmost(ids)

    private fun applyUnbound(update: CameraUpdate): CameraPosition {
        val b = binding
        return CameraMath.apply(
            storedCamera,
            update,
            b?.minZoom ?: CameraMath.MIN_ZOOM,
            b?.maxZoom ?: CameraMath.MAX_ZOOM,
        )
    }

    private companion object {
        const val DEFAULT_ANIMATION_DURATION_MS = 500L
    }
}
