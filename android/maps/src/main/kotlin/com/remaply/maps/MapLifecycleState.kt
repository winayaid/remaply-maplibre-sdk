package com.remaply.maps

/**
 * Lifecycle state of a map view. Spec: API-010.
 */
public enum class MapLifecycleState {
    /** Constructed; the native map has not been created yet. */
    INITIALIZED,

    /** Native map created, but not visible or not rendering. */
    CREATED,

    /** Visible and rendering. */
    STARTED,

    /** Visible, rendering, in the foreground and interactive. */
    RESUMED,

    /** Native resources released. Terminal state. */
    DESTROYED,
}
