package com.remaply.maps.sample

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remaply.maps.MapStyle
import com.remaply.maps.RemaplyMaps

/**
 * Sample launcher. Each entry is a small, self-contained screen; read its source next to the docs.
 *
 * Testing hooks (adb):
 * - `--ez parity true` opens the classic View sample running the parity script (docs/parity-checklist.md).
 * - `--es styleUrl <url>` / `--es styleAsset <path>` opens the Compose example with that style.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra(ViewSampleActivity.EXTRA_PARITY, false)) {
            startActivity(Intent(this, ViewSampleActivity::class.java).putExtra(ViewSampleActivity.EXTRA_PARITY, true))
            finish()
            return
        }
        intent.getStringExtra(EXTRA_SAMPLE_STYLE_ASSET)?.let { SampleScenario.style = MapStyle.bundled(it) }
        val overrideStyle =
            intent.getStringExtra(EXTRA_STYLE_URL)?.let(MapStyle::url)
                ?: intent.getStringExtra(EXTRA_STYLE_ASSET)?.let(MapStyle::bundled)
        setContent {
            SampleApp(
                initial = if (overrideStyle != null) Sample.FULL else null,
                style = overrideStyle ?: SampleScenario.style,
                openViewSample = { startActivity(Intent(this, ViewSampleActivity::class.java)) },
            )
        }
    }

    private companion object {
        const val EXTRA_STYLE_URL = "styleUrl"
        const val EXTRA_STYLE_ASSET = "styleAsset"
        const val EXTRA_SAMPLE_STYLE_ASSET = "sampleStyleAsset"
    }
}

/** The samples, in the order of the docs. */
enum class Sample(
    val title: String,
    val description: String,
) {
    BASIC("Basic map", "A style and a starting camera. Nothing else."),
    MARKERS("Markers and taps", "Add markers by tapping; tap a marker to select it."),
    CAMERA("Camera animation", "Animate, jump, rotate, tilt; see camera events."),
    FULL("Compose example", "The reference scenario with RemaplyMap."),
    VIEW("Classic View example", "The reference scenario with RemaplyMapView in XML."),
}

@Composable
private fun SampleApp(
    initial: Sample?,
    style: MapStyle,
    openViewSample: () -> Unit,
) {
    var current by rememberSaveable { mutableStateOf(initial) }
    BackHandler(enabled = current != null) { current = null }
    when (current) {
        null ->
            SampleList { sample ->
                if (sample == Sample.VIEW) openViewSample() else current = sample
            }
        Sample.BASIC -> BasicMapScreen()
        Sample.MARKERS -> MarkersAndTapsScreen()
        Sample.CAMERA -> CameraAnimationScreen()
        Sample.FULL -> FullScenarioScreen(style)
        Sample.VIEW -> Unit
    }
}

@Composable
private fun SampleList(onOpen: (Sample) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .safeDrawingPadding()
            .padding(16.dp),
    ) {
        BasicText(
            "Remaply Maps samples",
            style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = SampleColors.Text),
        )
        BasicText(
            "SDK ${RemaplyMaps.version} · MapLibre ${RemaplyMaps.mapLibreVersion}",
            style = TextStyle(fontSize = 13.sp, color = SampleColors.Secondary),
            modifier = Modifier.padding(bottom = 16.dp),
        )
        Sample.entries.forEach { sample ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(sample) }
                    .padding(vertical = 12.dp),
            ) {
                BasicText(sample.title, style = TextStyle(fontSize = 17.sp, color = SampleColors.Accent))
                BasicText(sample.description, style = TextStyle(fontSize = 14.sp, color = SampleColors.Secondary))
            }
        }
    }
}
