package com.remaply.maps.sample

import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.remaply.maps.CameraChangeReason
import com.remaply.maps.CameraPosition
import com.remaply.maps.CameraUpdate
import com.remaply.maps.ExperimentalRemaplyApi
import com.remaply.maps.GeoCoordinate
import com.remaply.maps.MapError
import com.remaply.maps.MapStyle
import com.remaply.maps.Marker
import com.remaply.maps.RemaplyMapView
import java.util.Locale

/**
 * Drives the SDK through a fixed sequence of public API calls and logs a normalized event trace
 * (tag `PARITY`). The iOS sample runs the same script; the two traces must be identical.
 * Used by the Phase 4 parity check (docs/parity-checklist.md).
 *
 * Start it with `adb shell am start -n com.remaply.maps.sample/.MainActivity --ez parity true`.
 */
class ParityScript(
    private val map: RemaplyMapView,
) {
    private val handler = Handler(Looper.getMainLooper())
    private var step = "load"
    private var pendingChanges = 0
    private var lastChange: Pair<CameraPosition, CameraChangeReason>? = null

    /** Camera values are omitted for this many upcoming change/idle lines (timing-dependent). */
    private var volatileLines = 0

    private val controller get() = map.controller

    // ---- Events from the map listener ----

    fun onStyleLoaded(style: MapStyle) = trace("styleLoaded ${describe(style)} markers=${controller.markers.size}")

    fun onMapReady() {
        trace("mapReady isMapReady=${controller.isMapReady}")
        runSteps()
    }

    fun onMapLoadError(error: MapError) = trace("loadError ${error.code}")

    fun onCameraChange(
        camera: CameraPosition,
        reason: CameraChangeReason,
    ) {
        pendingChanges++
        lastChange = camera to reason
    }

    fun onCameraIdle(camera: CameraPosition) = trace("idle ${format(camera)}")

    // ---- Steps ----

    private val steps: List<Pair<String, () -> Unit>> =
        listOf(
            "markers" to {
                SampleScenario.markers.forEach(controller::addMarker)
                trace("markers ${controller.markers.joinToString(",") { it.id }}")
            },
            "frame" to { controller.setCamera(PARIS_11) },
            "rendered" to { trace("rendered ${renderedMarkerIds()}") },
            "errors" to {
                expectError("addMarker(dup)") { controller.addMarker(Marker("a", GeoCoordinate(0.0, 0.0))) }
                expectError("addMarker(emptyId)") { controller.addMarker(Marker("", GeoCoordinate(0.0, 0.0))) }
                expectError("addMarker(badCoord)") { controller.addMarker(Marker("x", GeoCoordinate(0.0, 181.0))) }
                expectError("updateMarker(unknown)") { controller.updateMarker(Marker("zzz", GeoCoordinate(0.0, 0.0))) }
                trace("removeMarker(unknown)=${controller.removeMarker("zzz")}")
                expectError(
                    "setCamera(badCenter)",
                ) { controller.setCamera(CameraUpdate(center = GeoCoordinate(95.0, 0.0))) }
                expectError("setCamera(nanZoom)") { controller.setCamera(CameraUpdate(zoom = Double.NaN)) }
                expectError("animateCamera(negative)") { controller.animateCamera(CameraUpdate(zoom = 3.0), -1) }
            },
            "jump" to { controller.setCamera(PARIS_14) },
            "jumpSame" to { controller.setCamera(PARIS_14) },
            "jumpClamped" to { controller.setCamera(CameraUpdate(zoom = 30.0, tilt = 80.0, bearing = -90.0)) },
            "optionsClamp" to { map.options = map.options.copy(maxZoom = 13.0) },
            "optionsRestore" to { map.options = map.options.copy(maxZoom = 22.0) },
            "animate" to {
                controller.animateCamera(CameraUpdate(zoom = 12.0, bearing = 90.0, tilt = 30.0), 400) {
                    trace("completion $it")
                }
            },
            "cancel" to {
                volatileLines = 2 // the cancelled animation's changes and idle depend on timing
                controller.animateCamera(CameraUpdate(zoom = 13.0), 1000) { trace("completion $it") }
                handler.postDelayed({ controller.setCamera(CameraUpdate(zoom = 10.0)) }, 200)
            },
            "emptyAnimate" to { controller.animateCamera(CameraUpdate()) { trace("completion $it") } },
            "styleErrors" to {
                controller.setStyle(MapStyle.bundled("missing.json"))
                controller.setStyle(MapStyle.url("not-a-url"))
            },
            "markerAfterStyleError" to {
                // The rejected style requests above must not stop marker rendering (Phase 4 fix).
                controller.addMarker(Marker("d", GeoCoordinate(48.8700, 2.3600)))
                controller.setCamera(PARIS_11)
            },
            "renderedAfterStyleError" to { trace("rendered ${renderedMarkerIds()}") },
            "styleReload" to { controller.setStyle(MapStyle.bundled("styles/demo-bundled.json")) },
            "renderedAfterReload" to { trace("rendered ${renderedMarkerIds()}") },
            "styleSame" to { controller.setStyle(MapStyle.bundled("styles/demo-bundled.json")) },
            "markerEdit" to {
                controller.updateMarker(Marker("a", GeoCoordinate(48.8584, 2.2945), zIndex = 5))
                trace("getMarker(a).zIndex=${controller.getMarker("a")?.zIndex} markers=${controller.markers.size}")
                controller.removeAllMarkers()
                trace("markers=${controller.markers.size}")
            },
            "state" to {
                trace("isMapReady=${controller.isMapReady} style=${controller.style?.let(::describe)}")
                trace("camera ${format(controller.camera!!)}")
            },
        )

    private fun runSteps(index: Int = 0) {
        if (index >= steps.size) {
            trace("done")
            return
        }
        val (name, action) = steps[index]
        handler.postDelayed({
            step = name
            trace("begin")
            action()
            runSteps(index + 1)
        }, STEP_DELAY_MS)
    }

    // ---- Tracing ----

    private fun expectError(
        label: String,
        block: () -> Unit,
    ) {
        try {
            block()
            trace("$label no error")
        } catch (e: MapError) {
            trace("$label error ${e.code}")
        }
    }

    private fun trace(event: String) {
        flushChanges()
        emit(event)
    }

    private fun flushChanges() {
        val (camera, reason) = lastChange ?: return
        emit("change${if (pendingChanges > 1) "+" else ""} ${format(camera)} $reason")
        pendingChanges = 0
        lastChange = null
    }

    private fun emit(event: String) {
        Log.i(TAG, "$step|$event")
    }

    private fun format(camera: CameraPosition): String {
        if (volatileLines > 0) {
            volatileLines--
            return "(timing-dependent)"
        }
        return String.format(
            Locale.US,
            "z=%.2f b=%.1f t=%.1f lat=%.4f lng=%.4f",
            camera.zoom,
            camera.bearing,
            camera.tilt,
            camera.center.latitude,
            camera.center.longitude,
        )
    }

    /** Marker ids actually drawn on screen, via the experimental native escape hatch. */
    @OptIn(ExperimentalRemaplyApi::class)
    private fun renderedMarkerIds(): String {
        var ids = "(map not ready)"
        controller.withNativeMap { nativeMap, view ->
            val features =
                nativeMap.queryRenderedFeatures(
                    RectF(0f, 0f, view.width.toFloat(), view.height.toFloat()),
                    MARKER_LAYER_ID,
                )
            ids =
                features
                    .mapNotNull { it.getStringProperty(MARKER_ID_PROPERTY) }
                    .distinct()
                    .sorted()
                    .joinToString(",")
        }
        return ids
    }

    private fun describe(style: MapStyle): String =
        when (style) {
            is MapStyle.Url -> "url:${style.url}"
            is MapStyle.Bundled -> "bundled:${style.path}"
        }

    private companion object {
        const val TAG = "PARITY"
        const val STEP_DELAY_MS = 1500L

        // SDK-internal identifiers, used only by this test probe.
        const val MARKER_LAYER_ID = "remaply-markers-layer"
        const val MARKER_ID_PROPERTY = "remaply_id"
        val PARIS_11 = CameraUpdate(center = GeoCoordinate(48.8566, 2.3522), zoom = 11.0, bearing = 0.0, tilt = 0.0)
        val PARIS_14 = CameraUpdate(center = GeoCoordinate(48.8566, 2.3522), zoom = 14.0, bearing = 0.0, tilt = 0.0)
    }
}
