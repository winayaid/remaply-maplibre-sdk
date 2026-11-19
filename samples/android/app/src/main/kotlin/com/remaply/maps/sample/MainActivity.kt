package com.remaply.maps.sample

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remaply.maps.MapStyle
import com.remaply.maps.compose.RemaplyMap
import com.remaply.maps.compose.rememberMapController

/** Jetpack Compose sample: the reference scenario with `RemaplyMap`. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Parity check: `--ez parity true` opens the View sample running ParityScript.
        if (intent.getBooleanExtra(ViewSampleActivity.EXTRA_PARITY, false)) {
            startActivity(Intent(this, ViewSampleActivity::class.java).putExtra(ViewSampleActivity.EXTRA_PARITY, true))
            finish()
            return
        }
        // Manual-testing hook: `adb shell am start -n com.remaply.maps.sample/.MainActivity --es styleUrl <url>`
        // (or `--es styleAsset <path>`) loads a different style, for example to check error reporting.
        val style =
            intent.getStringExtra(EXTRA_STYLE_URL)?.let(MapStyle::url)
                ?: intent.getStringExtra(EXTRA_STYLE_ASSET)?.let(MapStyle::bundled)
                ?: SampleScenario.style
        setContent {
            ComposeSample(style, onOpenViewSample = { startActivity(Intent(this, ViewSampleActivity::class.java)) })
        }
    }

    private companion object {
        const val EXTRA_STYLE_URL = "styleUrl"
        const val EXTRA_STYLE_ASSET = "styleAsset"
    }
}

@Composable
private fun ComposeSample(
    style: MapStyle,
    onOpenViewSample: () -> Unit,
) {
    val controller = rememberMapController()
    val loading = stringResource(R.string.status_loading)
    var status by rememberSaveable { mutableStateOf(loading) }
    var camera by rememberSaveable { mutableStateOf("") }

    Box(Modifier.fillMaxSize()) {
        RemaplyMap(
            style = style,
            modifier = Modifier.fillMaxSize(),
            controller = controller,
            options = SampleScenario.options,
            onMapReady = {
                SampleScenario.addMarkersIfNeeded(it)
                status = "Map ready · ${it.markers.size} markers"
            },
            onMapLoadError = { status = "Load error: ${it.code}" },
            onMapTap = { status = "Map tap: ${SampleScenario.describe(it)}" },
            onMarkerTap = { status = "Marker tap: ${it.id}" },
            onCameraChange = { c, reason -> camera = SampleScenario.describe(c, reason) },
            onCameraIdle = { camera = "Idle · ${SampleScenario.describe(it)}" },
        )
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .safeDrawingPadding()
                .padding(12.dp)
                .fillMaxWidth()
                .background(Color(0xE6FFFFFF), RoundedCornerShape(8.dp))
                .padding(12.dp),
        ) {
            BasicText(status, style = TextStyle(fontSize = 15.sp, color = Color(0xFF202124)))
            BasicText(camera, style = TextStyle(fontSize = 13.sp, color = Color(0xFF5F6368)))
            Spacer(Modifier.width(8.dp))
            Row(Modifier.padding(top = 8.dp)) {
                SampleButton(stringResource(R.string.action_animate)) {
                    SampleScenario.animate(controller) { finished -> status = "Animation finished: $finished" }
                }
                SampleButton(stringResource(R.string.action_reset)) { SampleScenario.reset(controller) }
                SampleButton(stringResource(R.string.action_open_view_sample), onOpenViewSample)
            }
        }
    }
}

@Composable
private fun SampleButton(
    label: String,
    onClick: () -> Unit,
) {
    BasicText(
        label,
        style = TextStyle(fontSize = 14.sp, color = Color.White),
        modifier =
            Modifier
                .padding(end = 8.dp)
                .background(Color(0xFF00796B), RoundedCornerShape(6.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}
