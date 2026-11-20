package com.remaply.maps.internal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EventQueueTest {
    @Test
    fun eventsRunImmediatelyWhenIdle() {
        val queue = EventQueue()
        val log = mutableListOf<String>()
        queue.post { log += "a" }
        assertEquals(listOf("a"), log)
    }

    @Test
    fun eventsPostedInsideACallbackRunAfterItReturns() {
        val queue = EventQueue()
        val log = mutableListOf<String>()
        queue.post {
            log += "outer-start"
            queue.post { log += "inner" }
            log += "outer-end"
        }
        assertEquals(listOf("outer-start", "outer-end", "inner"), log)
    }

    @Test
    fun anAppExceptionPropagatesAndTheQueueRecovers() {
        val queue = EventQueue()
        val log = mutableListOf<String>()
        assertThrows(IllegalStateException::class.java) { queue.post { error("app bug") } }
        queue.post { log += "next" }
        assertEquals(listOf("next"), log)
    }
}
