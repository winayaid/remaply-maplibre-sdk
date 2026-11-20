package com.remaply.maps.internal

import android.view.Choreographer

/** Schedules per-frame callbacks. Replaceable in tests. */
internal interface FrameScheduler {
    fun postFrame(callback: (frameTimeNanos: Long) -> Unit): Any

    fun cancel(token: Any)
}

/** Production scheduler: one callback per display frame. */
internal object ChoreographerFrameScheduler : FrameScheduler {
    override fun postFrame(callback: (frameTimeNanos: Long) -> Unit): Any {
        val frameCallback = Choreographer.FrameCallback { callback(it) }
        Choreographer.getInstance().postFrameCallback(frameCallback)
        return frameCallback
    }

    override fun cancel(token: Any) {
        Choreographer.getInstance().removeFrameCallback(token as Choreographer.FrameCallback)
    }
}
