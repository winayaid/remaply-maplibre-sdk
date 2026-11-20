package com.remaply.maps

import android.graphics.Bitmap

/**
 * A marker drawn at a coordinate.
 *
 * Markers draw above every style layer, are never hidden by symbol collision, stay upright when
 * the map rotates or tilts, and survive style changes.
 *
 * Spec: API-006.
 *
 * @property id Non-empty and unique within a [MapController].
 * @property coordinate Where the marker is placed. Must be valid.
 * @property icon The image to draw.
 * @property anchor Which point of the icon sits on [coordinate].
 * @property zIndex Higher values draw on top. On equal values, the most recently added or updated
 *   marker draws on top.
 */
public data class Marker(
    public val id: String,
    public val coordinate: GeoCoordinate,
    public val icon: MarkerIcon = MarkerIcon.defaultPin(),
    public val anchor: MarkerAnchor = MarkerAnchor.BOTTOM,
    public val zIndex: Int = 0,
)

/**
 * The image drawn for a [Marker].
 *
 * Two icons are equal when both are the default pin, or when they wrap the same [Bitmap] instance.
 *
 * Spec: API-007.
 */
public class MarkerIcon private constructor(
    internal val bitmap: Bitmap?,
) {
    /** `true` when both are the default pin, or both wrap the same [Bitmap] instance. */
    override fun equals(other: Any?): Boolean = other is MarkerIcon && other.bitmap === bitmap

    /** Identity hash of the wrapped bitmap, consistent with [equals]. */
    override fun hashCode(): Int = System.identityHashCode(bitmap)

    /** `MarkerIcon(defaultPin)` or `MarkerIcon(image)`. */
    override fun toString(): String = if (bitmap == null) "MarkerIcon(defaultPin)" else "MarkerIcon(image)"

    /** Factories with the same call sites as on iOS. */
    public companion object {
        private val DEFAULT_PIN = MarkerIcon(null)

        /** The SDK's bundled pin, 28 × 40 dp, designed for [MarkerAnchor.BOTTOM]. */
        @JvmStatic
        public fun defaultPin(): MarkerIcon = DEFAULT_PIN

        /**
         * A custom [image], drawn at `width / (density / 160)` dp. A bitmap without a density
         * is treated as having the device density. The bitmap is not modified.
         */
        @JvmStatic
        public fun image(image: Bitmap): MarkerIcon = MarkerIcon(image)
    }
}

/**
 * The point of a marker's icon that is placed at the marker's coordinate. Spec: API-008.
 */
public enum class MarkerAnchor {
    /** Center of the icon. */
    CENTER,

    /** Middle of the top edge. */
    TOP,

    /** Middle of the bottom edge. The default, suited to pins. */
    BOTTOM,

    /** Middle of the left edge. */
    LEFT,

    /** Middle of the right edge. */
    RIGHT,

    /** Top-left corner. */
    TOP_LEFT,

    /** Top-right corner. */
    TOP_RIGHT,

    /** Bottom-left corner. */
    BOTTOM_LEFT,

    /** Bottom-right corner. */
    BOTTOM_RIGHT,
}
