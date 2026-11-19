package com.remaply.maps.internal

import com.remaply.maps.CameraPosition
import com.remaply.maps.GeoCoordinate
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.camera.CameraPosition as NativeCameraPosition

internal fun NativeCameraPosition.toSdk(): CameraPosition {
    val target = target ?: LatLng(0.0, 0.0)
    return CameraMath.normalize(
        CameraPosition(
            center = GeoCoordinate(target.latitude, target.longitude),
            zoom = zoom,
            bearing = bearing,
            tilt = tilt,
        ),
        minZoom = Double.NEGATIVE_INFINITY,
        maxZoom = Double.POSITIVE_INFINITY,
    )
}

internal fun CameraPosition.toNative(): NativeCameraPosition =
    NativeCameraPosition
        .Builder()
        .target(LatLng(center.latitude, center.longitude))
        .zoom(zoom)
        .bearing(bearing)
        .tilt(tilt)
        .build()

internal fun LatLng.toSdk(): GeoCoordinate = GeoCoordinate(latitude, CameraMath.wrapLongitude(longitude))
