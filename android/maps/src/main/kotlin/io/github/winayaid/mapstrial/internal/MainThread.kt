package io.github.winayaid.mapstrial.internal

import android.os.Looper
import io.github.winayaid.mapstrial.MapError
import io.github.winayaid.mapstrial.MapErrorCode

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
