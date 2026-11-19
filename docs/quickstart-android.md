# Quick start: Android

Show a map with a marker in a new or existing Android app. Allow about 10 minutes.

**You need:** Android Studio with AGP 9.x, `compileSdk = 37`, `minSdk` 24 or higher, and Kotlin 2.2 or newer.
You also need a map style URL. Examples here use the free, keyless
[OpenFreeMap](https://openfreemap.org) "Liberty" style; any MapLibre style works.

## 1. Add the dependency

> **Pre-release:** 0.1.0 isn't on Maven Central yet. Until it is, use [option B](#option-b-from-source).

### Option A: Maven Central (from 0.1.0)

In `settings.gradle.kts`, make sure `mavenCentral()` is in `dependencyResolutionManagement.repositories`. Then, in
your app module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.remaply:maps-android:0.1.0")
    // Only if you use Jetpack Compose:
    implementation("com.remaply:maps-android-compose:0.1.0")
}
```

### Option B: from source

1. Clone the SDK next to your project:

   ```bash
   git clone https://github.com/winayaid/remaply-maplibre-sdk.git
   ```

2. In your project's `settings.gradle.kts`, include the SDK build. Gradle then resolves the coordinates from
   option A to the source:

   ```kotlin
   includeBuild("../remaply-maplibre-sdk/android")
   ```

3. Add the same `dependencies` as in option A, **without the version**:

   ```kotlin
   implementation("com.remaply:maps-android")
   implementation("com.remaply:maps-android-compose") // Compose only
   ```

   Your project must use the same Android Gradle Plugin and Kotlin plugin versions as
   `remaply-maplibre-sdk/android/gradle/libs.versions.toml`.

No manifest changes are needed: the `INTERNET` permission comes with the SDK. The SDK removes MapLibre's
location permissions, because v1 doesn't use location.

## 2. Show a map

### Jetpack Compose

```kotlin
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.remaply.maps.CameraPosition
import com.remaply.maps.GeoCoordinate
import com.remaply.maps.MapOptions
import com.remaply.maps.MapStyle
import com.remaply.maps.Marker
import com.remaply.maps.compose.RemaplyMap
import com.remaply.maps.compose.rememberMapController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val paris = GeoCoordinate(48.8566, 2.3522)
        setContent {
            val controller = rememberMapController()
            RemaplyMap(
                style = MapStyle.url("https://tiles.openfreemap.org/styles/liberty"),
                modifier = Modifier.fillMaxSize(),
                controller = controller,
                options = MapOptions(initialCamera = CameraPosition(paris, zoom = 12.0)),
                onMapReady = {
                    // The controller survives rotation; only add the marker once.
                    if (it.getMarker("paris") == null) it.addMarker(Marker("paris", paris))
                },
                onMarkerTap = { marker -> println("Tapped ${marker.id}") },
            )
        }
    }
}
```

### Classic Views

Layout `res/layout/activity_main.xml`:

```xml
<com.remaply.maps.RemaplyMapView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/map"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:remaply_styleUrl="https://tiles.openfreemap.org/styles/liberty" />
```

Activity (uses AppCompat, which new Android Studio "Views" projects already include):

```kotlin
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.remaply.maps.GeoCoordinate
import com.remaply.maps.MapController
import com.remaply.maps.MapListener
import com.remaply.maps.Marker
import com.remaply.maps.RemaplyMapView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val map = findViewById<RemaplyMapView>(R.id.map)
        map.listener = object : MapListener {
            override fun onMapReady(controller: MapController) {
                if (controller.getMarker("paris") == null) {
                    controller.addMarker(Marker("paris", GeoCoordinate(48.8566, 2.3522)))
                }
            }

            override fun onMarkerTap(marker: Marker) {
                Toast.makeText(this@MainActivity, "Tapped ${marker.id}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
```

The view follows the Activity's lifecycle by itself; there's no need to forward `onStart`, `onStop`, and so on.
Give it an `android:id` so its markers survive rotation.

## 3. Run it

Run the app. You should see Paris with a red pin; tapping the pin calls `onMarkerTap`.

## Next steps

- [Usage guide](guide.md): camera, markers, events, errors, lifecycle, and logging.
- [Samples](../samples/android): basic map, markers and taps, camera animation, Compose and classic View.
- [Attribution](attribution.md): what your app must show and include.
- API reference: build it with `cd android && ./gradlew :dokkaGenerate` (see [API reference](api-reference.md)).
