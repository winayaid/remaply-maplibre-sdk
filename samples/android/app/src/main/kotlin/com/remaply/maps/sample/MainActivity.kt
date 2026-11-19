package com.remaply.maps.sample

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.remaply.maps.RemaplyMaps
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView

/**
 * Phase 1 foundation sample: shows a map with the demo style and nothing else.
 *
 * This temporarily drives MapLibre's `MapView` directly (reached through the SDK's `api`
 * dependency) to prove that the build, the pinned MapLibre version, and rendering all work.
 * Phase 2 replaces it with the SDK's `RemaplyMapView`, which handles the lifecycle forwarding
 * below on its own.
 */
class MainActivity : ComponentActivity() {
    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "Remaply Maps ${RemaplyMaps.version} (MapLibre ${RemaplyMaps.mapLibreVersion})")

        MapLibre.getInstance(this)
        mapView = MapView(this)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { map -> map.setStyle(DEMO_STYLE_URL) }
        setContentView(mapView)
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        mapView.onStop()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    @Deprecated("Deprecated in Java")
    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "RemaplySample"

        /** Free, keyless demo style (API_SPEC.md §16). The SDK never hardcodes a style. */
        const val DEMO_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
    }
}
