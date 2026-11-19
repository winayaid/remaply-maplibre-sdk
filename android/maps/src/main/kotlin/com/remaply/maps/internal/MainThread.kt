package com.remaply.maps.internal

import android.os.Looper
import com.remaply.maps.MapError
import com.remaply.maps.MapErrorCode

/** Enforces API_SPEC.md §3.4: every public call must be made on the main thread. */
internal object MainThread {
    /** Replaceable in unit tests, where there is no real main looper. */
    @Volatile
    var isMainThread: () -> Boolean = { Looper.myLooper() == Looper.getMainLooper() }

    @Throws(MapError::class)
    fun check(api: String) {
        if (!isMainThread()) {
            throw mapError(MapErrorCode.WRONG_THREAD, "$api must be called on the main thread")
        }
    }
}
