package com.remaply.maps

/**
 * Receives map events. Every method has an empty default implementation.
 *
 * Set it with [RemaplyMapView.listener]. The view holds it strongly and releases it in
 * [RemaplyMapView.onDestroy]. All callbacks run on the main thread and are never re-entered:
 * events caused by calls made inside a callback are delivered after the callback returns.
 *
 * Spec: API-040 to API-047.
 */
public interface MapListener {
    /**
     * The first style has loaded on this view, stored markers are rendered, and the stored camera
     * is applied. Fires once per view instance (so again on a new view after rotation).
     */
    public fun onMapReady(controller: MapController) {}

    /**
     * A style request failed, or the renderer could not start. Individual tile, sprite, or glyph
     * failures are not reported here; they are logged at [LogLevel.WARN].
     */
    public fun onMapLoadError(error: MapError) {}

    /** A style finished loading (including the first), after markers were re-added. */
    public fun onStyleLoaded(style: MapStyle) {}

    /** A confirmed single tap that did not hit a marker. */
    public fun onMapTap(coordinate: GeoCoordinate) {}

    /** A confirmed single tap on a marker. Only the topmost marker is reported, and [onMapTap] does not fire. */
    public fun onMarkerTap(marker: Marker) {}

    /** The camera changed. Fires at most once per rendered frame. */
    public fun onCameraChange(
        camera: CameraPosition,
        reason: CameraChangeReason,
    ) {}

    /** The camera stopped moving. */
    public fun onCameraIdle(camera: CameraPosition) {}
}
