package io.github.winayaid.mapstrial

import io.github.winayaid.mapstrial.internal.MainThread
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MapControllerTest {
    private val controller = MapController()

    @Before
    fun setUp() {
        MainThread.isMainThread = { true }
    }

    @After
    fun tearDown() {
        MainThread.isMainThread = { true }
    }

    // ---- Unbound behavior ----

    @Test
    fun freshControllerHasNoState() {
        assertFalse(controller.isMapReady)
        assertNull(controller.style)
        assertNull(controller.camera)
        assertTrue(controller.markers.isEmpty())
    }

    @Test
    fun setCameraWhileUnboundUpdatesStoredCamera() {
        controller.setCamera(CameraUpdate(center = coord(1.0, 2.0), zoom = 30.0))
        assertEquals(CameraPosition(coord(1.0, 2.0), 22.0), controller.camera)
        controller.setCamera(CameraUpdate(bearing = 370.0))
        assertEquals(10.0, controller.camera!!.bearing, 1e-9)
    }

    @Test
    fun animateWhileUnboundAppliesImmediatelyAndCompletesWithTrue() {
        val results = mutableListOf<Boolean>()
        controller.animateCamera(CameraUpdate(zoom = 4.0), 1000) { results += it }
        assertEquals(listOf(true), results)
        assertEquals(4.0, controller.camera!!.zoom, 1e-9)
    }

    @Test
    fun emptyUpdatesAreNoOps() {
        val results = mutableListOf<Boolean>()
        controller.setCamera(CameraUpdate())
        controller.animateCamera(CameraUpdate()) { results += it }
        assertNull(controller.camera)
        assertEquals(listOf(true), results)
    }

    @Test
    fun invalidCameraArgumentsThrowAndChangeNothing() {
        controller.setCamera(CameraUpdate(zoom = 3.0))
        val before = controller.camera
        assertMapError(MapErrorCode.INVALID_ARGUMENT) { controller.setCamera(CameraUpdate(center = coord(0.0, 200.0))) }
        assertMapError(MapErrorCode.INVALID_ARGUMENT) { controller.animateCamera(CameraUpdate(zoom = Double.NaN)) }
        assertMapError(MapErrorCode.INVALID_ARGUMENT) { controller.animateCamera(CameraUpdate(zoom = 5.0), -1) }
        assertEquals(before, controller.camera)
    }

    @Test
    fun setStyleIsStoredWhileUnbound() {
        controller.setStyle(MapStyle.url("https://example.com/a.json"))
        assertEquals(MapStyle.url("https://example.com/a.json"), controller.style)
    }

    @Test
    fun markerApiWhileUnbound() {
        controller.addMarker(Marker("a", coord(1.0, 1.0)))
        controller.addMarker(Marker("b", coord(2.0, 2.0)))
        assertEquals("a", controller.getMarker("a")?.id)
        assertMapError(MapErrorCode.DUPLICATE_MARKER_ID) { controller.addMarker(Marker("a", coord(0.0, 0.0))) }
        assertMapError(MapErrorCode.MARKER_NOT_FOUND) { controller.updateMarker(Marker("zzz", coord(0.0, 0.0))) }
        assertFalse(controller.removeMarker("zzz"))
        assertTrue(controller.removeMarker("a"))
        assertEquals(listOf("b"), controller.markers.map { it.id })
        controller.removeAllMarkers()
        assertTrue(controller.markers.isEmpty())
    }

    // ---- Bound behavior ----

    @Test
    fun boundReadyControllerForwardsToBinding() {
        val binding = FakeBinding()
        controller.bind(binding)
        assertTrue(controller.isMapReady)

        controller.setStyle(MapStyle.bundled("s.json"))
        assertEquals(listOf<MapStyle>(MapStyle.bundled("s.json")), binding.styleRequests)

        controller.setCamera(CameraUpdate(zoom = 8.0))
        assertEquals(8.0, binding.jumps.single().zoom, 1e-9)
        assertEquals(8.0, controller.camera!!.zoom, 1e-9)

        controller.animateCamera(CameraUpdate(bearing = 45.0), 250)
        val (target, duration, _) = binding.animations.single()
        assertEquals(45.0, target.bearing, 1e-9)
        assertEquals(8.0, target.zoom, 1e-9)
        assertEquals(250L, duration)

        controller.addMarker(Marker("a", coord(0.0, 0.0)))
        controller.updateMarker(Marker("a", coord(1.0, 0.0)))
        controller.removeMarker("a")
        controller.removeMarker("a") // no-op: no extra change
        controller.removeAllMarkers() // empty: no change
        assertEquals(3, binding.markerChanges)
    }

    @Test
    fun boundCameraIsClampedToBindingZoomRange() {
        val binding = FakeBinding(minZoom = 3.0, maxZoom = 10.0)
        controller.bind(binding)
        controller.setCamera(CameraUpdate(zoom = 20.0))
        assertEquals(10.0, binding.jumps.single().zoom, 1e-9)
    }

    @Test
    fun boundButNotReadyStoresCamera() {
        val binding = FakeBinding(ready = false)
        controller.bind(binding)
        controller.setCamera(CameraUpdate(zoom = 6.0))
        assertTrue(binding.jumps.isEmpty())
        assertEquals(6.0, controller.camera!!.zoom, 1e-9)
    }

    @Test
    fun unbindKeepsLastLiveCamera() {
        val binding = FakeBinding(camera = CameraPosition(coord(5.0, 5.0), 7.0))
        controller.bind(binding)
        controller.unbind(binding)
        assertFalse(controller.isMapReady)
        assertEquals(CameraPosition(coord(5.0, 5.0), 7.0), controller.camera)
    }

    @Test
    fun lastBindingWins() {
        val first = FakeBinding(camera = CameraPosition(coord(1.0, 1.0), 4.0))
        val second = FakeBinding(ready = false)
        controller.bind(first)
        controller.bind(second)
        assertTrue(first.unboundByController)
        assertSame(second, controller.binding)
        // The stale binding cannot unbind the new one.
        controller.unbind(first)
        assertSame(second, controller.binding)
        assertEquals(4.0, controller.camera!!.zoom, 1e-9)
    }

    // ---- Threading ----

    @Test
    fun backgroundThreadCallsThrowWrongThread() {
        MainThread.isMainThread = { false }
        assertMapError(MapErrorCode.WRONG_THREAD) { controller.setStyle(MapStyle.url("https://e.com/s.json")) }
        assertMapError(MapErrorCode.WRONG_THREAD) { controller.addMarker(Marker("a", coord(0.0, 0.0))) }
        assertMapError(MapErrorCode.WRONG_THREAD) { controller.getMarker("a") }
        MainThread.isMainThread = { true }
        assertTrue(controller.markers.isEmpty())
    }

    // ---- Callback ordering ----

    @Test
    fun completionFromInsideACallbackIsDeferredUntilItReturns() {
        val log = mutableListOf<String>()
        controller.animateCamera(CameraUpdate(zoom = 2.0), 100) {
            log += "first-start"
            controller.animateCamera(CameraUpdate(zoom = 3.0), 100) { log += "second" }
            log += "first-end"
        }
        assertEquals(listOf("first-start", "first-end", "second"), log)
    }
}
