package io.github.winayaid.mapstrial

import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.geometry.LatLng

/**
 * Instrumented tests (Phase 5): a real MapLibre map in an Activity, with HTTP stubbed in-process.
 * Spec items in brackets.
 */
@RunWith(AndroidJUnit4::class)
class MapInstrumentedTest {
    private lateinit var scenario: ActivityScenario<TestMapActivity>

    @Before
    fun setUp() {
        Events.clear()
        TestMapActivity.style = STUB_STYLE
        scenario = ActivityScenario.launch(TestMapActivity::class.java)
        waitUntil(message = "onMapReady") { Events.count("mapReady") == 1 }
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun loadsStyleAddsMarkerAndMovesCamera() {
        // Style loaded, then ready, with no camera events for the initial camera [API-041, §8.3].
        assertEquals(listOf("styleLoaded", "mapReady"), Events.log.toList())

        // Add a marker; it is drawn on the next frame [API-019, API-006].
        scenario.onMain { it.map.controller.addMarker(Marker("a", GeoCoordinate(48.8566, 2.3522))) }
        waitUntil(message = "marker drawn") { scenario.onMain { it.map.renderedMarkerIds() } == setOf("a") }

        // Jump: exactly one change and one idle [API-016].
        Events.clear()
        scenario.onMain { it.map.controller.setCamera(CameraUpdate(zoom = 6.0, bearing = 30.0)) }
        assertEquals(listOf("change PROGRAMMATIC", "idle"), Events.log.toList())
        assertEquals(
            6.0,
            scenario.onMain {
                it.map.controller.camera!!
                    .zoom
            },
            1e-6,
        )

        // Animate: several changes, one idle, then completion(true) [API-017].
        Events.clear()
        scenario.onMain {
            it.map.controller.animateCamera(CameraUpdate(zoom = 8.0), 300) { finished ->
                Events.log +=
                    "completion $finished"
            }
        }
        waitUntil(message = "animation completion") { Events.count("completion") == 1 }
        val log = Events.log.toList()
        assertTrue("expected several changes: $log", log.count { it == "change PROGRAMMATIC" } > 1)
        assertEquals(listOf("idle", "completion true"), log.takeLast(2))
        assertEquals(
            8.0,
            scenario.onMain {
                it.map.controller.camera!!
                    .zoom
            },
            1e-6,
        )
        assertEquals(setOf("a"), scenario.onMain { it.map.renderedMarkerIds() })
    }

    @Test
    fun tapsReportTheTopmostMarkerOrTheMap() {
        scenario.onMain {
            it.map.controller.addMarker(Marker("low", GeoCoordinate(48.8566, 2.3522), anchor = MarkerAnchor.CENTER))
            it.map.controller.addMarker(
                Marker("high", GeoCoordinate(48.8566, 2.3522), anchor = MarkerAnchor.CENTER, zIndex = 1),
            )
        }
        waitUntil(message = "markers drawn") { scenario.onMain { it.map.renderedMarkerIds() }.size == 2 }

        // Tap on the overlapping markers: only the topmost is reported, no map tap [API-045].
        tapAt(GeoCoordinate(48.8566, 2.3522))
        waitUntil(message = "marker tap") { Events.count("markerTap") == 1 }
        assertEquals(1, Events.count("markerTap high"))
        assertEquals(0, Events.count("mapTap"))

        // Tap away from the markers (lower-right of the view): a map tap [API-044].
        Events.clear()
        scenario.onMain { tapAtPoint(it.map, it.map.width * 0.85f, it.map.height * 0.85f) }
        waitUntil(message = "map tap") { Events.count("mapTap") == 1 }
        assertEquals(0, Events.count("markerTap"))
    }

    @Test
    fun touchDuringAnimationCancelsIt() {
        scenario.onMain {
            it.map.controller.animateCamera(CameraUpdate(zoom = 10.0), 5_000) { finished ->
                Events.log +=
                    "completion $finished"
            }
        }
        SystemClock.sleep(200)
        scenario.onMain {
            val now = SystemClock.uptimeMillis()
            val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 10f, 10f, 0)
            it.map.dispatchTouchEvent(down)
            down.recycle()
        }
        waitUntil(message = "cancel completion") { Events.count("completion") == 1 }
        // The cancelled movement is closed with an idle before completion(false) [§8.3].
        assertEquals(
            listOf("idle", "completion false"),
            Events.log.toList().filter {
                it == "idle" ||
                    it.startsWith("completion")
            },
        )
    }

    @Test
    fun styleErrorsAreReportedWithTheirCodes() {
        Events.clear()
        scenario.onMain { it.map.controller.setStyle(MapStyle.url("not-a-url")) }
        waitUntil(message = "invalid url error") { Events.count("loadError") == 1 }
        scenario.onMain { it.map.controller.setStyle(MapStyle.bundled("missing.json")) }
        waitUntil(message = "missing asset error") { Events.count("loadError") == 2 }
        scenario.onMain { it.map.controller.setStyle(MapStyle.url(STUB_404_URL)) }
        waitUntil(message = "404 error") { Events.count("loadError") == 3 }
        assertEquals(
            listOf("loadError INVALID_ARGUMENT", "loadError STYLE_NOT_FOUND", "loadError STYLE_LOAD_FAILED"),
            Events.log.filter { it.startsWith("loadError") },
        )
        // A bundled style from assets loads and keeps the markers [API-004, API-043].
        scenario.onMain { it.map.controller.addMarker(Marker("m", GeoCoordinate(48.8566, 2.3522))) }
        scenario.onMain { it.map.controller.setStyle(MapStyle.bundled("styles/test-style.json")) }
        waitUntil(message = "bundled style loaded") { Events.count("styleLoaded") == 1 }
        waitUntil(message = "marker drawn") { scenario.onMain { it.map.renderedMarkerIds() } == setOf("m") }
    }

    @Test
    fun recreationKeepsTheControllerWithItsMarkersAndCamera() {
        val before =
            scenario.onMain {
                it.map.controller.addMarker(Marker("a", GeoCoordinate(1.0, 2.0)))
                it.map.controller.setCamera(CameraUpdate(zoom = 7.0))
                it.map.controller
            }
        scenario.recreate() // configuration change, such as rotation [§9.2]
        waitUntil(message = "onMapReady after recreate") { Events.count("mapReady") == 2 }
        scenario.onMain {
            assertSame(before, it.map.controller)
            assertEquals(
                listOf("a"),
                it.map.controller.markers
                    .map(Marker::id),
            )
            assertEquals(
                7.0,
                it.map.controller.camera!!
                    .zoom,
                1e-6,
            )
        }
    }

    @Test
    fun savedStateRestoresStyleAndCameraIntoAFreshController() {
        // Simulates process death: state saved by one view, restored into a new view with a new controller [API-051].
        val saved = Bundle()
        scenario.onMain {
            it.map.controller.setCamera(CameraUpdate(zoom = 9.0, bearing = 45.0))
            it.map.onSaveInstanceState(saved)
        }
        scenario.onMain {
            val restored = RemaplyMapView(it, null, MapOptions(), MapController())
            restored.onCreate(saved)
            assertEquals(STUB_STYLE, restored.controller.style)
            assertEquals(9.0, restored.controller.camera!!.zoom, 1e-6)
            assertEquals(45.0, restored.controller.camera!!.bearing, 1e-6)
            restored.onDestroy()
        }
    }

    @Test
    fun lifecycleFollowsTheActivity() {
        scenario.moveToState(Lifecycle.State.STARTED)
        assertEquals(MapLifecycleState.STARTED, scenario.onMain { it.map.lifecycleState })
        scenario.moveToState(Lifecycle.State.CREATED)
        assertEquals(MapLifecycleState.CREATED, scenario.onMain { it.map.lifecycleState })
        scenario.moveToState(Lifecycle.State.RESUMED)
        assertEquals(MapLifecycleState.RESUMED, scenario.onMain { it.map.lifecycleState })
        val map = scenario.onMain { it.map }
        val controller = map.controller
        scenario.close()
        assertEquals(MapLifecycleState.DESTROYED, map.lifecycleState)
        assertFalse(controller.isMapReady)
        assertNotNull(controller.camera) // the controller keeps its state [§6.6]
    }

    private fun tapAt(coordinate: GeoCoordinate) {
        scenario.onMain {
            var point: android.graphics.PointF? = null
            @OptIn(ExperimentalRemaplyApi::class)
            it.map.controller.withNativeMap { map, _ ->
                point = map.projection.toScreenLocation(LatLng(coordinate.latitude, coordinate.longitude))
            }
            val p = checkNotNull(point)
            tapAtPoint(it.map, p.x, p.y)
        }
    }

    private fun tapAtPoint(
        view: RemaplyMapView,
        x: Float,
        y: Float,
    ) {
        val down = SystemClock.uptimeMillis()
        val ev1 = MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN, x, y, 0)
        val ev2 = MotionEvent.obtain(down, down + 50, MotionEvent.ACTION_UP, x, y, 0)
        view.dispatchTouchEvent(ev1)
        view.dispatchTouchEvent(ev2)
        ev1.recycle()
        ev2.recycle()
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun installStub() = StubHttp.install()
    }
}
