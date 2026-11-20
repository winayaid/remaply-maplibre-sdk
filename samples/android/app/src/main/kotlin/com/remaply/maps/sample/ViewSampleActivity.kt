package com.remaply.maps.sample

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.remaply.maps.CameraChangeReason
import com.remaply.maps.CameraPosition
import com.remaply.maps.GeoCoordinate
import com.remaply.maps.MapController
import com.remaply.maps.MapError
import com.remaply.maps.MapListener
import com.remaply.maps.MapStyle
import com.remaply.maps.Marker
import com.remaply.maps.RemaplyMapView

/**
 * Classic View sample: the reference scenario with `RemaplyMapView` inflated from XML.
 * Lifecycle is automatic; no forwarding of onStart/onStop/... is needed.
 */
class ViewSampleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_sample)

        val map = findViewById<RemaplyMapView>(R.id.map)
        val status = findViewById<TextView>(R.id.status)
        map.options = SampleScenario.options
        val parity = if (intent.getBooleanExtra(EXTRA_PARITY, false)) ParityScript(map) else null

        map.listener =
            object : MapListener {
                override fun onStyleLoaded(style: MapStyle) {
                    parity?.onStyleLoaded(style)
                }

                override fun onMapReady(controller: MapController) {
                    if (parity != null) {
                        parity.onMapReady()
                        return
                    }
                    SampleScenario.addMarkersIfNeeded(controller)
                    val count = controller.markers.size
                    status.text = resources.getQuantityString(R.plurals.status_ready, count, count)
                }

                override fun onMapLoadError(error: MapError) {
                    parity?.onMapLoadError(error)
                    status.text = getString(R.string.status_error, error.code.name)
                }

                override fun onMapTap(coordinate: GeoCoordinate) {
                    status.text = getString(R.string.status_map_tap, SampleScenario.describe(coordinate))
                }

                override fun onMarkerTap(marker: Marker) {
                    status.text = getString(R.string.status_marker_tap, marker.id)
                }

                override fun onCameraChange(
                    camera: CameraPosition,
                    reason: CameraChangeReason,
                ) {
                    parity?.onCameraChange(camera, reason)
                }

                override fun onCameraIdle(camera: CameraPosition) {
                    parity?.onCameraIdle(camera)
                    status.text = SampleScenario.describe(camera)
                }
            }

        findViewById<Button>(R.id.animate).setOnClickListener {
            SampleScenario.animate(map.controller) { finished ->
                status.text = getString(R.string.status_animation, finished.toString())
            }
        }
        findViewById<Button>(R.id.reset).setOnClickListener { SampleScenario.reset(map.controller) }
    }

    companion object {
        /** Boolean extra: run [ParityScript] instead of the interactive scenario. */
        const val EXTRA_PARITY = "parity"
    }
}
