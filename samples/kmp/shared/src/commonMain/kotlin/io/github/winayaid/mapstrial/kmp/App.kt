package io.github.winayaid.mapstrial.kmp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToLong

/** Any MapLibre style works; this one is free and needs no key. */
const val MAP_STYLE_URL: String = "https://tiles.openfreemap.org/styles/liberty"

data class Place(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

/** A camera move requested by the shared UI. [requestId] changes on every request, so the same place can repeat. */
data class CameraTarget(
    val latitude: Double,
    val longitude: Double,
    val zoom: Double,
    val requestId: Int,
)

val samplePlaces: List<Place> =
    listOf(
        Place("paris", "Paris", 48.8566, 2.3522),
        Place("berlin", "Berlin", 52.5200, 13.4050),
        Place("rome", "Rome", 41.9028, 12.4964),
        Place("madrid", "Madrid", 40.4168, -3.7038),
    )

/**
 * The native map of each platform: Android uses the SDK's Compose `RemaplyMap`, iOS hosts the SDK's
 * `RemaplyMapView` from Swift (see iosMain and iosApp/RemaplyNativeMap.swift).
 */
@Composable
expect fun PlatformMap(
    places: List<Place>,
    cameraTarget: CameraTarget,
    onMapReady: () -> Unit,
    onPlaceTap: (placeId: String) -> Unit,
    onMapTap: (latitude: Double, longitude: Double) -> Unit,
    modifier: Modifier = Modifier,
)

/** The whole screen, shared by Android and iOS. */
@Composable
fun App() {
    var status by rememberSaveable { mutableStateOf(LOADING) }
    var cameraTarget by remember { mutableStateOf(CameraTarget(48.0, 6.0, 3.5, requestId = 0)) }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        PlatformMap(
            places = samplePlaces,
            cameraTarget = cameraTarget,
            // Called again when the map view is recreated (for example on rotation); keep the latest status then.
            onMapReady = { if (status == LOADING) status = "Map ready. Tap a marker or pick a city." },
            onPlaceTap = { id -> status = "Marker tapped: ${samplePlaces.first { it.id == id }.name}" },
            onMapTap = { lat, lon -> status = "Map tap: ${lat.format()}, ${lon.format()}" },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFFF2F4F7))
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BasicText(status, Modifier.testTag("status"), style = TextStyle(fontSize = 15.sp, color = Color(0xFF1D2939)))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                samplePlaces.forEach { place ->
                    CityButton(place.name) {
                        cameraTarget = CameraTarget(place.latitude, place.longitude, 11.0, cameraTarget.requestId + 1)
                        status = "Flying to ${place.name}…"
                    }
                }
            }
        }
    }
}

@Composable
private fun CityButton(
    label: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .background(Color(0xFF1F6FEB), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        BasicText(label, style = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium))
    }
}

private const val LOADING = "Loading the map…"

private fun Double.format(): String = ((this * 10_000).roundToLong() / 10_000.0).toString()
