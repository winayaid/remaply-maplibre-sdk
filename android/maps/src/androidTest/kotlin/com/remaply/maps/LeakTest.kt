package com.remaply.maps

import android.os.Debug
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger

/**
 * Repeated create/destroy cycles must not leak (API_SPEC.md §9.4).
 *
 * - Java objects: every RemaplyMapView, MapLibre MapView, and Activity must be collectable.
 * - Native memory: MapLibre itself retains native memory per map instance on the emulator
 *   (measured: ~239 MB over 50 plain MapLibre MapViews), so the SDK is measured against a plain
 *   MapLibre baseline run in the same process. The SDK may add at most [MAX_SDK_OVERHEAD_MB].
 */
@RunWith(AndroidJUnit4::class)
// The API 24 CI emulator (software GL) crashes when many MapLibre GL surfaces are created in quick
// succession (EGL_BAD_MATCH from the emulator's EGL layer), with or without the SDK. The cycles run on
// API 26+; API 24–25 needs a physical-device check (docs/test-report.md, open issues).
@SdkSuppress(minSdkVersion = 26)
class LeakTest {
    /** 50 map views created, made ready, and destroyed inside one Activity, bound to one long-lived controller. */
    @Test
    fun fiftyMapViewCyclesLeaveNothingBehind() {
        TestMapActivity.style = STUB_STYLE
        Events.clear()
        val views = mutableListOf<WeakReference<RemaplyMapView>>()
        val nativeViews = mutableListOf<WeakReference<MapView>>()
        val controller = MapController() // as if held by a ViewModel
        val (baselineMb, sdkMb) =
            ActivityScenario.launch(TestMapActivity::class.java).use { scenario ->
                waitUntil(message = "host map ready") { Events.count("mapReady") >= 1 }
                val baseline = measureNativeGrowth { repeat(VIEW_CYCLES) { plainMapLibreCycle(scenario) } }
                val sdk =
                    measureNativeGrowth {
                        repeat(VIEW_CYCLES) { cycle -> sdkCycle(scenario, controller, cycle, views, nativeViews) }
                    }
                baseline to sdk
            }
        settleGc()
        Log.i(
            TAG,
            "native growth over $VIEW_CYCLES cycles: plain MapLibre %.1f MB, SDK %.1f MB".format(baselineMb, sdkMb),
        )
        assertEquals(VIEW_CYCLES, nativeViews.size)
        assertEquals("RemaplyMapViews still reachable", 0, views.count { it.get() != null })
        assertEquals("MapLibre MapViews still reachable", 0, nativeViews.count { it.get() != null })
        assertFalse("the controller keeps no binding", controller.isMapReady)
        assertTrue(
            "SDK native growth %.1f MB exceeds MapLibre baseline %.1f MB by more than %.0f MB"
                .format(sdkMb, baselineMb, MAX_SDK_OVERHEAD_MB),
            sdkMb - baselineMb < MAX_SDK_OVERHEAD_MB,
        )
    }

    /** 10 full Activity create/destroy cycles. */
    @Test
    fun activityCyclesLeaveNothingBehind() {
        TestMapActivity.style = STUB_STYLE
        val activities = mutableListOf<WeakReference<TestMapActivity>>()
        val views = mutableListOf<WeakReference<RemaplyMapView>>()
        repeat(ACTIVITY_CYCLES) { cycle -> activityCycle(cycle, activities, views) }
        settleGc()
        assertEquals("activities still reachable", 0, activities.count { it.get() != null })
        assertEquals("RemaplyMapViews still reachable", 0, views.count { it.get() != null })
    }

    // Each cycle runs in its own function so no stack slot of the test method keeps a view alive.

    private fun sdkCycle(
        scenario: ActivityScenario<TestMapActivity>,
        controller: MapController,
        cycle: Int,
        views: MutableList<WeakReference<RemaplyMapView>>,
        nativeViews: MutableList<WeakReference<MapView>>,
    ) {
        val ready = AtomicInteger()
        scenario.onMain { activity ->
            val view = RemaplyMapView(activity, STUB_STYLE, MapOptions(), controller)
            view.listener =
                object : MapListener {
                    override fun onMapReady(controller: MapController) {
                        ready.incrementAndGet()
                    }
                }
            activity.addContentView(view, FrameLayout.LayoutParams(SIZE_PX, SIZE_PX))
            views += WeakReference(view)
        }
        waitUntil(message = "map ready in view cycle $cycle") { ready.get() >= 1 }
        scenario.onMain {
            @OptIn(ExperimentalRemaplyApi::class)
            controller.withNativeMap { _, native -> nativeViews += WeakReference(native) }
            val view = views.last().get()!!
            view.onDestroy()
            (view.parent as ViewGroup).removeView(view)
        }
    }

    private fun plainMapLibreCycle(scenario: ActivityScenario<TestMapActivity>) {
        val loaded = AtomicInteger()
        var ref: WeakReference<MapView>? = null
        scenario.onMain { activity ->
            val view = MapView(activity)
            view.onCreate(null)
            view.onStart()
            view.onResume()
            view.getMapAsync { map ->
                map.setStyle(Style.Builder().fromUri(STUB_STYLE_URL)) { loaded.incrementAndGet() }
            }
            activity.addContentView(view, FrameLayout.LayoutParams(SIZE_PX, SIZE_PX))
            ref = WeakReference(view)
        }
        waitUntil(message = "plain MapLibre style") { loaded.get() >= 1 }
        scenario.onMain {
            val view = ref!!.get()!!
            view.onPause()
            view.onStop()
            view.onDestroy()
            (view.parent as ViewGroup).removeView(view as View)
        }
    }

    private fun activityCycle(
        cycle: Int,
        activities: MutableList<WeakReference<TestMapActivity>>,
        views: MutableList<WeakReference<RemaplyMapView>>,
    ) {
        Events.clear()
        ActivityScenario.launch(TestMapActivity::class.java).use { scenario ->
            // The system may recreate the Activity; onMapReady then fires again (spec §9.2).
            waitUntil(message = "onMapReady in activity cycle $cycle") { Events.count("mapReady") >= 1 }
            scenario.onMain { activity ->
                activities += WeakReference(activity)
                views += WeakReference(activity.map)
            }
        }
    }

    private fun measureNativeGrowth(block: () -> Unit): Double {
        settleGc()
        val before = Debug.getNativeHeapAllocatedSize()
        block()
        settleGc()
        return (Debug.getNativeHeapAllocatedSize() - before) / BYTES_PER_MB
    }

    private fun settleGc() {
        repeat(5) {
            Runtime.getRuntime().gc()
            System.runFinalization()
            Thread.sleep(200)
        }
    }

    companion object {
        private const val TAG = "LeakTest"
        const val VIEW_CYCLES = 50
        const val ACTIVITY_CYCLES = 10
        const val SIZE_PX = 400
        const val MAX_SDK_OVERHEAD_MB = 16.0
        const val BYTES_PER_MB = 1024.0 * 1024.0

        @JvmStatic
        @BeforeClass
        fun installStub() = StubHttp.install()
    }
}
