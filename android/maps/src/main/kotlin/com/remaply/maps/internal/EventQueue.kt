package com.remaply.maps.internal

/**
 * Delivers callbacks in order and never re-entrantly (API_SPEC.md §8.3 rule 5).
 *
 * If an event is posted while another is being delivered (for example, the app calls the
 * controller from inside a callback), it runs after the current callback returns.
 */
internal class EventQueue {
    private val pending = ArrayDeque<() -> Unit>()
    private var draining = false

    fun post(event: () -> Unit) {
        pending.addLast(event)
        if (draining) return
        draining = true
        try {
            while (pending.isNotEmpty()) pending.removeFirst().invoke()
        } finally {
            draining = false
        }
    }

    fun clear() = pending.clear()
}
