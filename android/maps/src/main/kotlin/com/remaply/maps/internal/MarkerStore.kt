package com.remaply.maps.internal

import com.remaply.maps.MapError
import com.remaply.maps.MapErrorCode
import com.remaply.maps.Marker

/**
 * The controller's marker collection and its ordering rules (API_SPEC.md §5.6, §6.5).
 *
 * Validation order, identical on both platforms:
 * empty id → invalid coordinate → duplicate / not found.
 */
internal class MarkerStore {
    private data class Entry(
        val marker: Marker,
        val sequence: Long,
    )

    // Insertion order is the public `markers` order; an update keeps the marker's position.
    private val entries = LinkedHashMap<String, Entry>()
    private var nextSequence = 0L

    val all: List<Marker> get() = entries.values.map { it.marker }

    val isEmpty: Boolean get() = entries.isEmpty()

    operator fun get(id: String): Marker? = entries[id]?.marker

    @Throws(MapError::class)
    fun add(marker: Marker) {
        validate(marker, "addMarker")
        if (entries.containsKey(marker.id)) {
            throw mapError(
                MapErrorCode.DUPLICATE_MARKER_ID,
                "addMarker: a marker with id '${marker.id}' already exists",
            )
        }
        entries[marker.id] = Entry(marker, nextSequence++)
    }

    @Throws(MapError::class)
    fun update(marker: Marker) {
        validate(marker, "updateMarker")
        if (!entries.containsKey(marker.id)) {
            throw mapError(MapErrorCode.MARKER_NOT_FOUND, "updateMarker: no marker with id '${marker.id}'")
        }
        entries[marker.id] = Entry(marker, nextSequence++)
    }

    fun remove(id: String): Boolean = entries.remove(id) != null

    fun clear() = entries.clear()

    /** Markers from bottom to top: ascending zIndex, then ascending add/update order. */
    fun drawOrder(): List<Marker> =
        entries.values
            .sortedWith(compareBy<Entry>({ it.marker.zIndex }, { it.sequence }))
            .map { it.marker }

    /** The topmost marker among [ids] (unknown ids are ignored), or `null`. */
    fun topmost(ids: Collection<String>): Marker? =
        ids
            .mapNotNull { entries[it] }
            .maxWithOrNull(compareBy<Entry>({ it.marker.zIndex }, { it.sequence }))
            ?.marker

    private fun validate(
        marker: Marker,
        api: String,
    ) {
        if (marker.id.isEmpty()) throw mapError(MapErrorCode.INVALID_ARGUMENT, "$api: marker id must not be empty")
        if (!marker.coordinate.isValid) {
            throw mapError(MapErrorCode.INVALID_ARGUMENT, "$api: invalid coordinate ${marker.coordinate}")
        }
    }
}
