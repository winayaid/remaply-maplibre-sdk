package io.github.winayaid.mapstrial.internal

import android.content.Context
import org.maplibre.android.MapLibre

/** One-time MapLibre initialization, done lazily on first use (API_SPEC.md §12). */
internal object MapLibreBridge {
    fun ensureInitialized(context: Context) {
        MapLibre.getInstance(context.applicationContext)
        MapLibreLogBridge.install()
    }
}
