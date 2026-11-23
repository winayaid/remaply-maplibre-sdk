package io.github.winayaid.mapstrial.kmp

import platform.UIKit.UIView

// The Remaply iOS SDK is a Swift package, and Kotlin/Native can only call Objective-C APIs. So the iOS app
// implements these interfaces in Swift with RemaplyMapView (iosApp/RemaplyNativeMap.swift) and passes the
// factory to MainViewController. Kotlin interfaces appear in Swift as protocols.

/** Creates native maps. Implemented in Swift. */
interface NativeMapFactory {
    fun createMap(
        styleUrl: String,
        initialCamera: CameraTarget,
        places: List<Place>,
        listener: NativeMapListener,
    ): NativeMap
}

/** One native map. Implemented in Swift. */
interface NativeMap {
    /** The view Compose shows, a RemaplyMapView. */
    val view: UIView

    fun flyTo(target: CameraTarget)
}

/** Map events from Swift back to the shared code. */
interface NativeMapListener {
    fun onMapReady()

    fun onPlaceTap(placeId: String)

    fun onMapTap(
        latitude: Double,
        longitude: Double,
    )
}
