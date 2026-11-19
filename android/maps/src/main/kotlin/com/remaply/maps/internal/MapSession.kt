package com.remaply.maps.internal

import android.content.Context
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import com.remaply.maps.CameraChangeReason
import com.remaply.maps.CameraPosition
import com.remaply.maps.GeoCoordinate
import com.remaply.maps.MapController
import com.remaply.maps.MapError
import com.remaply.maps.MapErrorCode
import com.remaply.maps.MapOptions
import com.remaply.maps.MapStyle
import com.remaply.maps.Marker
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.io.IOException
import kotlin.math.roundToInt

/**
 * One native map inside one view: owns the MapLibre `MapView` and implements everything the
 * controller asks of a view ([MapBinding]) and the lifecycle steps ([LifecycleStateMachine.Target]).
 *
 * Camera events: programmatic moves (setCamera, animations, zoom-limit clamps) are reported by
 * this class directly, so their events are deterministic and identical to iOS. Native MapLibre
 * camera callbacks are only used for user gestures.
 */
@Suppress("TooManyFunctions")
internal class MapSession(
    private val context: Context,
    private val container: FrameLayout,
    private val controller: MapController,
    initialOptions: MapOptions,
    private val sink: EventSink,
    private val frames: FrameScheduler = ChoreographerFrameScheduler,
) : MapBinding,
    LifecycleStateMachine.Target {
    /** Receives view-level events. Only called while this session is bound and alive. */
    interface EventSink {
        fun onMapReady()

        fun onMapLoadError(error: MapError)

        fun onStyleLoaded(style: MapStyle)

        fun onMapTap(coordinate: GeoCoordinate)

        fun onMarkerTap(marker: Marker)

        fun onCameraChange(
            camera: CameraPosition,
            reason: CameraChangeReason,
        )

        fun onCameraIdle(camera: CameraPosition)
    }

    private enum class StyleState { NONE, LOADING, LOADED, FAILED }

    private class RunningAnimation(
        val animation: CameraAnimation,
        val completion: ((Boolean) -> Unit)?,
    ) {
        var startNanos: Long = -1
        var frameToken: Any? = null
    }

    private var mapView: MapView? = null
    private var map: MapLibreMap? = null
    private var options: MapOptions = sanitize(initialOptions)
    private val handler = Handler(Looper.getMainLooper())
    private val markerLayer = MarkerLayer(context)
    private val density = context.resources.displayMetrics.density

    private var ready = false
    private var destroyed = false
    private var unboundByController = false

    private var styleState = StyleState.NONE
    private var activeStyle: MapStyle? = null
    private var styleGeneration = 0

    private var markerSyncPosted = false
    private var running: RunningAnimation? = null

    /** > 0 while this class moves the camera itself; native callbacks are ignored then. */
    private var programmaticDepth = 0

    private val gate =
        CameraEventGate(
            emitChange = { camera, reason -> postEvent { sink.onCameraChange(camera, reason) } },
            emitIdle = { camera -> postEvent { sink.onCameraIdle(camera) } },
        )

    private val onFailLoading = MapView.OnDidFailLoadingMapListener { message -> onNativeLoadFailure(message) }
    private val onCameraMove = MapLibreMap.OnCameraMoveListener { onNativeCamera(idle = false) }
    private val onCameraIdle = MapLibreMap.OnCameraIdleListener { onNativeCamera(idle = true) }
    private val onMapClick = MapLibreMap.OnMapClickListener { latLng -> handleTap(latLng) }

    // ---- MapBinding ----

    override val isReady: Boolean get() = ready && !destroyed && !unboundByController
    override val minZoom: Double get() = options.minZoom
    override val maxZoom: Double get() = options.maxZoom
    override val nativeMap: MapLibreMap? get() = map
    override val nativeView: MapView? get() = mapView

    override fun currentCamera(): CameraPosition =
        map?.cameraPosition?.toSdk() ?: controller.storedCamera ?: CameraMath.DEFAULT_CAMERA

    override fun requestStyle(style: MapStyle) {
        if (destroyed || unboundByController) return
        if (style == activeStyle && (styleState == StyleState.LOADING || styleState == StyleState.LOADED)) return
        loadStyle(style)
    }

    override fun jumpTo(target: CameraPosition) {
        if (!isReady) return
        cancelAnimation()
        moveNative(target)
        gate.idle(currentCamera(), CameraChangeReason.PROGRAMMATIC)
    }

    override fun animateTo(
        target: CameraPosition,
        durationMs: Long,
        completion: ((Boolean) -> Unit)?,
    ) {
        if (!isReady) {
            completion?.let { c -> controller.events.post { c(false) } }
            return
        }
        cancelAnimation()
        val run = RunningAnimation(CameraAnimation(currentCamera(), target, durationMs), completion)
        running = run
        scheduleFrame(run)
    }

    override fun cancelAnimation() {
        val run = running ?: return
        running = null
        run.frameToken?.let(frames::cancel)
        // Close the movement sequence, then report the cancellation (API_SPEC.md §6.4, §8.3).
        gate.idle(currentCamera(), CameraChangeReason.PROGRAMMATIC)
        run.completion?.let { c -> controller.events.post { c(false) } }
    }

    override fun markersChanged() {
        if (!isReady || markerSyncPosted) return
        markerSyncPosted = true
        handler.post {
            markerSyncPosted = false
            if (isReady) syncMarkers()
        }
    }

    override fun onUnboundByController() {
        cancelAnimation()
        unboundByController = true
        gate.disable()
        map?.style?.let { style ->
            if (style.isFullyLoaded) markerLayer.sync(style, emptyList())
        }
    }

    // ---- Lifecycle (driven by LifecycleStateMachine) ----

    override fun performCreate() {
        MapLibreBridge.ensureInitialized(context)
        val view =
            try {
                MapView(context)
            } catch (
                @Suppress("TooGenericExceptionCaught") e: RuntimeException,
            ) {
                val error = mapError(MapErrorCode.RENDERER_INIT_FAILED, "Could not create the MapLibre map view", e)
                postEvent { sink.onMapLoadError(error) }
                return
            }
        mapView = view
        container.addView(
            view,
            0,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        view.onCreate(null)
        view.addOnDidFailLoadingMapListener(onFailLoading)
        if (controller.storedCamera == null) {
            options.initialCamera?.let {
                controller.storedCamera =
                    CameraMath.normalize(it, options.minZoom, options.maxZoom)
            }
        }
        controller.bind(this)
        view.getMapAsync(::onMapAvailable)
    }

    override fun performStart() {
        mapView?.onStart()
    }

    override fun performResume() {
        mapView?.onResume()
    }

    override fun performPause() {
        mapView?.onPause()
    }

    override fun performStop() {
        mapView?.onStop()
    }

    override fun performDestroy() {
        cancelAnimation()
        controller.unbind(this)
        destroyed = true
        ready = false
        gate.disable()
        handler.removeCallbacksAndMessages(null)
        map?.let { m ->
            m.removeOnCameraMoveListener(onCameraMove)
            m.removeOnCameraIdleListener(onCameraIdle)
            m.removeOnMapClickListener(onMapClick)
        }
        mapView?.let { view ->
            view.removeOnDidFailLoadingMapListener(onFailLoading)
            view.onDestroy()
            container.removeView(view)
        }
        markerLayer.reset()
        map = null
        mapView = null
    }

    fun onLowMemory() {
        mapView?.onLowMemory()
    }

    // ---- Options ----

    fun applyOptions(newOptions: MapOptions) {
        options = sanitize(newOptions)
        map?.let(::applyOptionsTo)
    }

    private fun applyOptionsTo(m: MapLibreMap) {
        val ui = m.uiSettings
        ui.isScrollGesturesEnabled = options.scrollGesturesEnabled
        ui.isZoomGesturesEnabled = options.zoomGesturesEnabled
        ui.isDoubleTapGesturesEnabled = options.zoomGesturesEnabled
        ui.isQuickZoomGesturesEnabled = options.zoomGesturesEnabled
        ui.isRotateGesturesEnabled = options.rotateGesturesEnabled
        ui.isTiltGesturesEnabled = options.tiltGesturesEnabled
        ui.isCompassEnabled = options.compassEnabled
        programmatic {
            m.setMinZoomPreference(options.minZoom)
            m.setMaxZoomPreference(options.maxZoom)
            val camera = m.cameraPosition.toSdk()
            if (camera.zoom !in options.minZoom..options.maxZoom) {
                moveNative(camera.copy(zoom = camera.zoom.coerceIn(options.minZoom, options.maxZoom)))
            }
        }
        // If the new limits moved the camera, report it as a programmatic change (API_SPEC.md §7.5).
        if (isReady && running == null) gate.idle(currentCamera(), CameraChangeReason.PROGRAMMATIC)
    }

    private fun sanitize(o: MapOptions): MapOptions {
        val (fixed, warnings) = CameraMath.sanitize(o)
        warnings.forEach { SdkLog.warn("MapOptions: $it") }
        return fixed
    }

    // ---- Map setup ----

    private fun onMapAvailable(m: MapLibreMap) {
        if (destroyed) return
        map = m
        configureControls(m)
        m.setMaxPitchPreference(CameraMath.MAX_TILT)
        applyOptionsTo(m)
        m.addOnCameraMoveListener(onCameraMove)
        m.addOnCameraIdleListener(onCameraIdle)
        m.addOnMapClickListener(onMapClick)
        controller.storedCamera?.let(::moveNative)
        controller.style?.let(::loadStyle)
    }

    private fun configureControls(m: MapLibreMap) {
        val margin = (CONTROL_MARGIN_DP * density).roundToInt()
        val ui = m.uiSettings
        ui.compassGravity = Gravity.TOP or Gravity.END
        ui.setCompassMargins(0, margin, margin, 0)
        ui.setCompassFadeFacingNorth(true)
        ui.isLogoEnabled = true
        ui.logoGravity = Gravity.BOTTOM or Gravity.START
        ui.setLogoMargins(margin, 0, 0, margin)
        ui.isAttributionEnabled = true
        ui.attributionGravity = Gravity.BOTTOM or Gravity.END
        ui.setAttributionMargins(0, 0, margin, margin)
    }

    // ---- Style loading (API_SPEC.md §6.2, §8.2) ----

    private fun loadStyle(style: MapStyle) {
        val m = map ?: return // Loaded once the native map is available.
        val generation = ++styleGeneration
        activeStyle = style
        styleState = StyleState.LOADING
        StyleValidation.problem(style)?.let { (code, message) ->
            failStyle(generation, mapError(code, message))
            return
        }
        val uri =
            when (style) {
                is MapStyle.Url -> style.url.trim()
                is MapStyle.Bundled -> {
                    if (!assetExists(style.path)) {
                        failStyle(
                            generation,
                            mapError(
                                MapErrorCode.STYLE_NOT_FOUND,
                                "Bundled style not found in assets: '${style.path}'",
                            ),
                        )
                        return
                    }
                    "asset://${style.path}"
                }
            }
        markerLayer.reset()
        m.setStyle(Style.Builder().fromUri(uri)) { loaded ->
            if (generation == styleGeneration && !destroyed) onStyleLoaded(loaded, style)
        }
    }

    private fun assetExists(path: String): Boolean =
        try {
            context.assets.open(path).close()
            true
        } catch (_: IOException) {
            false
        }

    private fun onNativeLoadFailure(message: String) {
        if (styleState == StyleState.LOADING) {
            failStyle(styleGeneration, mapError(MapErrorCode.STYLE_LOAD_FAILED, "Style failed to load: $message"))
        } else {
            // Tiles, sprites, glyphs: not a style failure (API_SPEC.md §8.2).
            SdkLog.warn("Map resource failed to load: $message")
        }
    }

    private fun failStyle(
        generation: Int,
        error: MapError,
    ) {
        if (generation != styleGeneration) return
        styleState = StyleState.FAILED
        postEvent { sink.onMapLoadError(error) }
    }

    private fun onStyleLoaded(
        style: Style,
        requested: MapStyle,
    ) {
        if (unboundByController) return
        styleState = StyleState.LOADED
        markerLayer.install(style)
        markerLayer.sync(style, controller.markersInDrawOrder())
        val first = !ready
        if (first) {
            controller.storedCamera?.let(::moveNative)
            ready = true
            gate.enable(currentCamera())
        }
        postEvent { sink.onStyleLoaded(requested) }
        if (first) postEvent { sink.onMapReady() }
    }

    private fun syncMarkers() {
        // Render into whichever style is on screen. A style request rejected before reaching MapLibre
        // (INVALID_ARGUMENT, STYLE_NOT_FOUND) leaves the previous style and its marker layer in place;
        // while a new style loads, the layer is reset and sync is a no-op until it is re-installed.
        val style = map?.style ?: return
        if (style.isFullyLoaded) markerLayer.sync(style, controller.markersInDrawOrder())
    }

    // ---- Camera ----

    private fun moveNative(target: CameraPosition) {
        val m = map ?: return
        programmatic { m.moveCamera(CameraUpdateFactory.newCameraPosition(target.toNative())) }
    }

    private inline fun programmatic(block: () -> Unit) {
        programmaticDepth++
        try {
            block()
        } finally {
            programmaticDepth--
        }
    }

    private fun onNativeCamera(idle: Boolean) {
        // Only user gestures are reported from native callbacks; see the class comment.
        if (!isReady || programmaticDepth > 0 || running != null) return
        if (idle) {
            gate.idle(currentCamera(), CameraChangeReason.USER_GESTURE)
        } else {
            gate.change(currentCamera(), CameraChangeReason.USER_GESTURE)
        }
    }

    private fun scheduleFrame(run: RunningAnimation) {
        run.frameToken = frames.postFrame { nanos -> onAnimationFrame(run, nanos) }
    }

    private fun onAnimationFrame(
        run: RunningAnimation,
        frameTimeNanos: Long,
    ) {
        if (running !== run || !isReady) return
        if (run.startNanos < 0) run.startNanos = frameTimeNanos
        val elapsedMs = (frameTimeNanos - run.startNanos) / NANOS_PER_MILLI
        moveNative(run.animation.valueAt(elapsedMs))
        if (run.animation.isFinished(elapsedMs)) {
            running = null
            gate.idle(currentCamera(), CameraChangeReason.PROGRAMMATIC)
            run.completion?.let { c -> controller.events.post { c(true) } }
        } else {
            gate.change(currentCamera(), CameraChangeReason.PROGRAMMATIC)
            scheduleFrame(run)
        }
    }

    // ---- Taps (API_SPEC.md §8.2) ----

    private fun handleTap(latLng: LatLng): Boolean {
        val m = map ?: return false
        if (!isReady) return true
        val point = m.projection.toScreenLocation(latLng)
        val radius = TAP_RADIUS_DP * density
        val hits =
            m.queryRenderedFeatures(
                RectF(point.x - radius, point.y - radius, point.x + radius, point.y + radius),
                MarkerLayer.LAYER_ID,
            )
        val marker = controller.topmostMarker(hits.mapNotNull { it.getStringProperty(MarkerLayer.PROP_ID) })
        if (marker != null) {
            postEvent { sink.onMarkerTap(marker) }
        } else {
            val coordinate = latLng.toSdk()
            postEvent { sink.onMapTap(coordinate) }
        }
        return true
    }

    // ---- Events ----

    /** Delivers a view event, unless this session was destroyed or unbound in the meantime. */
    private fun postEvent(event: () -> Unit) {
        controller.events.post {
            if (!destroyed && !unboundByController) event()
        }
    }

    private companion object {
        const val CONTROL_MARGIN_DP = 8f
        const val TAP_RADIUS_DP = 8f
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
