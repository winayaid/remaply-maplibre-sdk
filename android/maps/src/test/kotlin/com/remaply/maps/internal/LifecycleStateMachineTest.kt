package com.remaply.maps.internal

import com.remaply.maps.MapLifecycleState.CREATED
import com.remaply.maps.MapLifecycleState.DESTROYED
import com.remaply.maps.MapLifecycleState.INITIALIZED
import com.remaply.maps.MapLifecycleState.RESUMED
import com.remaply.maps.MapLifecycleState.STARTED
import org.junit.Assert.assertEquals
import org.junit.Test

class LifecycleStateMachineTest {
    private val calls = mutableListOf<String>()
    private val machine =
        LifecycleStateMachine(
            object : LifecycleStateMachine.Target {
                override fun performCreate() {
                    calls += "create"
                }

                override fun performStart() {
                    calls += "start"
                }

                override fun performResume() {
                    calls += "resume"
                }

                override fun performPause() {
                    calls += "pause"
                }

                override fun performStop() {
                    calls += "stop"
                }

                override fun performDestroy() {
                    calls += "destroy"
                }
            },
        )

    @Test
    fun skippedStepsArePerformedInOrder() {
        machine.moveUpTo(RESUMED)
        assertEquals(listOf("create", "start", "resume"), calls)
        assertEquals(RESUMED, machine.state)
    }

    @Test
    fun repeatedAndLowerUpCallsAreIgnored() {
        machine.moveUpTo(RESUMED)
        calls.clear()
        machine.moveUpTo(RESUMED)
        machine.moveUpTo(STARTED)
        machine.moveUpTo(CREATED)
        assertEquals(emptyList<String>(), calls)
    }

    @Test
    fun downCallsStepDownAndIgnoreLowerStates() {
        machine.moveUpTo(RESUMED)
        calls.clear()
        machine.moveDownTo(CREATED)
        assertEquals(listOf("pause", "stop"), calls)
        machine.moveDownTo(STARTED)
        assertEquals(CREATED, machine.state)
        assertEquals(listOf("pause", "stop"), calls)
    }

    @Test
    fun downCallsBeforeCreateDoNothing() {
        machine.moveDownTo(CREATED)
        assertEquals(INITIALIZED, machine.state)
        assertEquals(emptyList<String>(), calls)
    }

    @Test
    fun destroyFromResumedStepsDownThenDestroys() {
        machine.moveUpTo(RESUMED)
        calls.clear()
        machine.destroy()
        assertEquals(listOf("pause", "stop", "destroy"), calls)
        assertEquals(DESTROYED, machine.state)
    }

    @Test
    fun destroyBeforeCreateReleasesNothing() {
        machine.destroy()
        assertEquals(emptyList<String>(), calls)
        assertEquals(DESTROYED, machine.state)
    }

    @Test
    fun everyCallAfterDestroyIsIgnored() {
        machine.moveUpTo(STARTED)
        machine.destroy()
        calls.clear()
        machine.moveUpTo(RESUMED)
        machine.moveDownTo(CREATED)
        machine.destroy()
        assertEquals(emptyList<String>(), calls)
        assertEquals(DESTROYED, machine.state)
    }
}
