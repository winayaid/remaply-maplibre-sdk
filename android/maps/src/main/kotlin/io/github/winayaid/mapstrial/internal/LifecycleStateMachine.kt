package io.github.winayaid.mapstrial.internal

import io.github.winayaid.mapstrial.MapLifecycleState
import io.github.winayaid.mapstrial.MapLifecycleState.CREATED
import io.github.winayaid.mapstrial.MapLifecycleState.DESTROYED
import io.github.winayaid.mapstrial.MapLifecycleState.INITIALIZED
import io.github.winayaid.mapstrial.MapLifecycleState.RESUMED
import io.github.winayaid.mapstrial.MapLifecycleState.STARTED

/**
 * The lifecycle state machine of API_SPEC.md §9.1: idempotent and order-tolerant.
 *
 * - Up-calls (create, start, resume) perform any missing steps and ignore calls at or above the target.
 * - Down-calls (pause, stop) step down and ignore calls at or below the target.
 * - Destroy steps all the way down, then releases. Every call after that is ignored and logged.
 */
internal class LifecycleStateMachine(
    private val target: Target,
) {
    interface Target {
        fun performCreate()

        fun performStart()

        fun performResume()

        fun performPause()

        fun performStop()

        fun performDestroy()
    }

    var state: MapLifecycleState = INITIALIZED
        private set

    fun moveUpTo(desired: MapLifecycleState): Boolean {
        if (rejectIfDestroyed("move to $desired")) return false
        while (state < desired) {
            when (state) {
                INITIALIZED -> target.performCreate().also { state = CREATED }
                CREATED -> target.performStart().also { state = STARTED }
                STARTED -> target.performResume().also { state = RESUMED }
                RESUMED, DESTROYED -> return true
            }
        }
        return true
    }

    fun moveDownTo(desired: MapLifecycleState): Boolean {
        if (rejectIfDestroyed("move to $desired")) return false
        while (state > desired && state > CREATED) {
            when (state) {
                RESUMED -> target.performPause().also { state = STARTED }
                STARTED -> target.performStop().also { state = CREATED }
                else -> return true
            }
        }
        return true
    }

    fun destroy() {
        if (rejectIfDestroyed("destroy")) return
        moveDownTo(CREATED)
        if (state == CREATED) target.performDestroy()
        state = DESTROYED
    }

    private fun rejectIfDestroyed(action: String): Boolean {
        if (state != DESTROYED) return false
        SdkLog.warn("MAP_DESTROYED: cannot $action; the map view was already destroyed")
        return true
    }
}
