package io.github.winayaid.mapstrial

import android.graphics.RectF
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import io.github.winayaid.mapstrial.internal.MarkerLayer
import io.github.winayaid.mapstrial.test.R
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.maplibre.android.MapLibre
import org.maplibre.android.module.http.HttpRequestUtil
import java.util.concurrent.CopyOnWriteArrayList

/** Background-only style served by [StubHttp]; it needs no tiles, sprites, or glyphs. */
internal const val STUB_STYLE_URL = "https://remaply.test/style.json"
internal const val STUB_404_URL = "https://remaply.test/missing-style.json"
internal val STUB_STYLE = MapStyle.url(STUB_STYLE_URL)

private const val STUB_STYLE_JSON =
    """{"version":8,"sources":{},"layers":[{"id":"bg","type":"background","paint":{"background-color":"#DDE7EE"}}]}"""

/** Answers MapLibre's HTTP requests in-process, so instrumented tests never touch the network. */
internal object StubHttp {
    fun install() {
        // MapLibre's HTTP layer reads the MapLibre instance when it is first used.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync { MapLibre.getInstance(instrumentation.targetContext) }
        val client =
            OkHttpClient
                .Builder()
                .addInterceptor(
                    Interceptor { chain ->
                        val request = chain.request()
                        val (code, body) =
                            when (request.url.toString()) {
                                STUB_STYLE_URL -> 200 to STUB_STYLE_JSON
                                else -> 404 to "not found"
                            }
                        Response
                            .Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(code)
                            .message(if (code == 200) "OK" else "Not Found")
                            .body(body.toResponseBody("application/json".toMediaType()))
                            .build()
                    },
                ).build()
        HttpRequestUtil.setOkHttpClient(client)
    }
}

/** Every listener event, as text, across activity instances. */
internal object Events {
    val log = CopyOnWriteArrayList<String>()

    fun clear() = log.clear()

    fun count(prefix: String) = log.count { it.startsWith(prefix) }
}

internal object RecordingListener : MapListener {
    override fun onMapReady(controller: MapController) {
        Events.log += "mapReady"
    }

    override fun onMapLoadError(error: MapError) {
        Events.log += "loadError ${error.code}"
    }

    override fun onStyleLoaded(style: MapStyle) {
        Events.log += "styleLoaded"
    }

    override fun onMapTap(coordinate: GeoCoordinate) {
        Events.log += "mapTap"
    }

    override fun onMarkerTap(marker: Marker) {
        Events.log += "markerTap ${marker.id}"
    }

    override fun onCameraChange(
        camera: CameraPosition,
        reason: CameraChangeReason,
    ) {
        Events.log += "change $reason"
    }

    override fun onCameraIdle(camera: CameraPosition) {
        Events.log += "idle"
    }
}

/** Hosts a [RemaplyMapView] with a stable id, like an app screen. */
class TestMapActivity : ComponentActivity() {
    lateinit var map: RemaplyMapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        map = RemaplyMapView(this, style, MapOptions(initialCamera = START_CAMERA))
        map.id = R.id.test_map
        map.listener = RecordingListener
        setContentView(map)
    }

    companion object {
        var style: MapStyle? = STUB_STYLE
        val START_CAMERA = CameraPosition(GeoCoordinate(48.8566, 2.3522), zoom = 4.0)
    }
}

/** Polls [condition] until it holds or [timeoutMs] passes. */
internal fun waitUntil(
    timeoutMs: Long = 15_000,
    message: String = "condition",
    condition: () -> Boolean,
) {
    val deadline = SystemClock.uptimeMillis() + timeoutMs
    while (!condition()) {
        if (SystemClock.uptimeMillis() >
            deadline
        ) {
            throw AssertionError("Timed out waiting for $message. Events: ${Events.log}")
        }
        SystemClock.sleep(50)
    }
}

internal fun <T> ActivityScenario<TestMapActivity>.onMain(block: (TestMapActivity) -> T): T {
    var result: Result<T>? = null
    onActivity { result = runCatching { block(it) } }
    return result!!.getOrThrow()
}

/** Marker ids actually drawn on screen. */
@OptIn(ExperimentalRemaplyApi::class)
internal fun RemaplyMapView.renderedMarkerIds(): Set<String> {
    var ids = emptySet<String>()
    controller.withNativeMap { map, view ->
        ids =
            map
                .queryRenderedFeatures(RectF(0f, 0f, view.width.toFloat(), view.height.toFloat()), MarkerLayer.LAYER_ID)
                .mapNotNull { it.getStringProperty(MarkerLayer.PROP_ID) }
                .toSet()
    }
    return ids
}
