package io.github.winayaid.mapstrial.internal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.DisplayMetrics
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import io.github.winayaid.mapstrial.Marker
import io.github.winayaid.mapstrial.MarkerAnchor
import io.github.winayaid.mapstrial.R
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import java.util.IdentityHashMap

/**
 * Renders markers as an SDK-managed GeoJSON source and symbol layer on top of the style
 * (API_SPEC.md §5.6, decision Q3). The same structure is used on iOS.
 */
internal class MarkerLayer(
    private val context: Context,
) {
    private var source: GeoJsonSource? = null
    private val registeredImages = IdentityHashMap<Bitmap, String>()
    private var nextImageId = 0
    private var defaultPin: Bitmap? = null

    /** Adds the source, layer, and default pin to a freshly loaded [style]. */
    fun install(style: Style) {
        registeredImages.clear()
        style.addImage(DEFAULT_ICON_ID, defaultPinBitmap())
        val newSource = GeoJsonSource(SOURCE_ID)
        // MapLibre Android applies GeoJSON updates on a worker thread by default, and an update can be
        // lost when it races with other map work. Markers are few (v1 target: 1,000), so apply updates
        // synchronously: every marker change is on screen by the next frame, as on iOS (API_SPEC.md §6.5).
        newSource.setOverrideSynchronousUpdate(true)
        style.addSource(newSource)
        style.addLayer(
            SymbolLayer(LAYER_ID, SOURCE_ID).withProperties(
                PropertyFactory.iconImage(Expression.get(PROP_ICON)),
                PropertyFactory.iconAnchor(Expression.get(PROP_ANCHOR)),
                PropertyFactory.symbolSortKey(Expression.get(PROP_SORT)),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_VIEWPORT),
                PropertyFactory.iconPitchAlignment(Property.ICON_PITCH_ALIGNMENT_VIEWPORT),
            ),
        )
        source = newSource
    }

    /** Replaces the rendered markers with [markers], given bottom to top. */
    fun sync(
        style: Style,
        markers: List<Marker>,
    ) {
        val source = source ?: return
        val used = IdentityHashMap<Bitmap, String>()
        val features =
            markers.mapIndexed { index, marker ->
                val bitmap = marker.icon.bitmap
                val iconId =
                    if (bitmap ==
                        null
                    ) {
                        DEFAULT_ICON_ID
                    } else {
                        imageIdFor(style, bitmap).also { used[bitmap] = it }
                    }
                Feature.fromGeometry(Point.fromLngLat(marker.coordinate.longitude, marker.coordinate.latitude)).apply {
                    addStringProperty(PROP_ID, marker.id)
                    addStringProperty(PROP_ICON, iconId)
                    addStringProperty(PROP_ANCHOR, marker.anchor.toNative())
                    addNumberProperty(PROP_SORT, index)
                }
            }
        // Drop images no marker uses any more, so replaced icons do not accumulate in the style.
        val unused = registeredImages.entries.filter { it.key !in used }
        unused.forEach { (bitmap, id) ->
            style.removeImage(id)
            registeredImages.remove(bitmap)
        }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    /** Forgets the current style's objects (the style itself is being discarded). */
    fun reset() {
        source = null
        registeredImages.clear()
    }

    private fun imageIdFor(
        style: Style,
        bitmap: Bitmap,
    ): String =
        registeredImages.getOrPut(bitmap) {
            val id = "$IMAGE_ID_PREFIX${nextImageId++}"
            style.addImage(id, withDensity(bitmap))
            id
        }

    /**
     * MapLibre derives the image's pixel ratio from `Bitmap.density`. A bitmap without a density
     * is treated as device density (API_SPEC.md §5.7); copy it so the app's bitmap is untouched.
     */
    private fun withDensity(bitmap: Bitmap): Bitmap {
        if (bitmap.density != Bitmap.DENSITY_NONE) return bitmap
        val copy = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false)
        copy.density = context.resources.displayMetrics.densityDpi
        return copy
    }

    private fun defaultPinBitmap(): Bitmap =
        defaultPin ?: run {
            val drawable =
                checkNotNull(ContextCompat.getDrawable(context, R.drawable.remaply_default_pin)) {
                    "Missing default pin drawable"
                }
            val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
            drawable.setBounds(0, 0, bitmap.width, bitmap.height)
            drawable.draw(Canvas(bitmap))
            bitmap.density =
                context.resources.displayMetrics.densityDpi
                    .takeIf { it > 0 } ?: DisplayMetrics.DENSITY_DEFAULT
            bitmap.also { defaultPin = it }
        }

    companion object {
        const val SOURCE_ID: String = "remaply-markers-source"
        const val LAYER_ID: String = "remaply-markers-layer"
        const val PROP_ID: String = "remaply_id"
        private const val PROP_ICON = "remaply_icon"
        private const val PROP_ANCHOR = "remaply_anchor"
        private const val PROP_SORT = "remaply_sort"
        private const val DEFAULT_ICON_ID = "remaply-default-pin"
        private const val IMAGE_ID_PREFIX = "remaply-icon-"

        fun MarkerAnchor.toNative(): String =
            when (this) {
                MarkerAnchor.CENTER -> Property.ICON_ANCHOR_CENTER
                MarkerAnchor.TOP -> Property.ICON_ANCHOR_TOP
                MarkerAnchor.BOTTOM -> Property.ICON_ANCHOR_BOTTOM
                MarkerAnchor.LEFT -> Property.ICON_ANCHOR_LEFT
                MarkerAnchor.RIGHT -> Property.ICON_ANCHOR_RIGHT
                MarkerAnchor.TOP_LEFT -> Property.ICON_ANCHOR_TOP_LEFT
                MarkerAnchor.TOP_RIGHT -> Property.ICON_ANCHOR_TOP_RIGHT
                MarkerAnchor.BOTTOM_LEFT -> Property.ICON_ANCHOR_BOTTOM_LEFT
                MarkerAnchor.BOTTOM_RIGHT -> Property.ICON_ANCHOR_BOTTOM_RIGHT
            }
    }
}
