package io.github.winayaid.mapstrial

import io.github.winayaid.mapstrial.internal.CameraMath
import io.github.winayaid.mapstrial.internal.MapBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

/** Asserts that [block] throws a [MapError] with [code]. */
internal inline fun assertMapError(
    code: MapErrorCode,
    block: () -> Unit,
): MapError {
    try {
        block()
    } catch (e: MapError) {
        assertEquals(code, e.code)
        return e
    }
    fail("Expected MapError($code)")
    throw AssertionError()
}

internal fun coord(
    lat: Double,
    lng: Double,
) = GeoCoordinate(lat, lng)

/** A binding that records what the controller asks of it, with a synchronous "map". */
internal class FakeBinding(
    var ready: Boolean = true,
    var camera: CameraPosition = CameraPosition(coord(0.0, 0.0), 1.0),
    override val minZoom: Double = CameraMath.MIN_ZOOM,
    override val maxZoom: Double = CameraMath.MAX_ZOOM,
) : MapBinding {
    val styleRequests = mutableListOf<MapStyle>()
    val jumps = mutableListOf<CameraPosition>()
    val animations = mutableListOf<Triple<CameraPosition, Long, ((Boolean) -> Unit)?>>()
    var markerChanges = 0
    var cancelCount = 0
    var unboundByController = false

    override val isReady: Boolean get() = ready

    override fun currentCamera(): CameraPosition = camera

    override fun requestStyle(style: MapStyle) {
        styleRequests += style
    }

    override fun jumpTo(target: CameraPosition) {
        jumps += target
        camera = target
    }

    override fun animateTo(
        target: CameraPosition,
        durationMs: Long,
        completion: ((Boolean) -> Unit)?,
    ) {
        animations += Triple(target, durationMs, completion)
    }

    override fun cancelAnimation() {
        cancelCount++
    }

    override fun markersChanged() {
        markerChanges++
    }

    override fun onUnboundByController() {
        unboundByController = true
    }

    override val nativeMap: MapLibreMap? = null
    override val nativeView: MapView? = null
}
