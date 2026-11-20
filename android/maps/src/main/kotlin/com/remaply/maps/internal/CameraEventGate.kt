package com.remaply.maps.internal

import com.remaply.maps.CameraChangeReason
import com.remaply.maps.CameraPosition

/**
 * Turns raw camera movement into the event contract of API_SPEC.md §8.2–8.3:
 * one or more `onCameraChange` events, then exactly one `onCameraIdle`, with duplicates dropped.
 *
 * Disabled until the map is ready, so the initial camera never produces events.
 */
internal class CameraEventGate(
    private val emitChange: (CameraPosition, CameraChangeReason) -> Unit,
    private val emitIdle: (CameraPosition) -> Unit,
) {
    private var enabled = false
    private var lastEmitted: CameraPosition? = null
    private var inSequence = false

    /** Starts emitting events. [current] is the baseline: no change is reported for it. */
    fun enable(current: CameraPosition) {
        enabled = true
        lastEmitted = current
        inSequence = false
    }

    fun disable() {
        enabled = false
        inSequence = false
    }

    val isMoving: Boolean get() = inSequence

    /** The camera is at [position]. Emits a change unless it equals the last reported one. */
    fun change(
        position: CameraPosition,
        reason: CameraChangeReason,
    ) {
        if (!enabled || position == lastEmitted) return
        lastEmitted = position
        inSequence = true
        emitChange(position, reason)
    }

    /** The camera stopped at [position]. Emits a final change if needed, then one idle. */
    fun idle(
        position: CameraPosition,
        reason: CameraChangeReason,
    ) {
        if (!enabled) return
        change(position, reason)
        if (!inSequence) return
        inSequence = false
        emitIdle(position)
    }
}
