package com.universalreader.app.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackQueueTest {

    @Test
    fun emptyQueueIsFinished() {
        val queue = PlaybackQueue(emptyList())
        assertEquals(-1, queue.index)
        assertNull(queue.current)
        assertFalse(queue.hasNext)
        assertFalse(queue.hasPrevious)
        assertTrue(queue.isFinished)
        assertFalse(queue.advance())
        assertFalse(queue.previous())
        assertFalse(queue.jumpTo(0))
    }

    @Test
    fun advancesAndRewinds() {
        val queue = PlaybackQueue(listOf("một", "hai", "ba"))
        assertEquals("một", queue.current)
        assertTrue(queue.hasNext)
        assertTrue(queue.advance())
        assertEquals("hai", queue.current)
        assertTrue(queue.previous())
        assertEquals("một", queue.current)
        assertFalse(queue.previous())
    }

    @Test
    fun startIndexIsClampedAndJumpIsBounded() {
        assertEquals(2, PlaybackQueue(listOf("a", "b", "c"), startIndex = 99).index)
        assertEquals(0, PlaybackQueue(listOf("a", "b", "c"), startIndex = -5).index)

        val queue = PlaybackQueue(listOf("a", "b", "c"))
        assertTrue(queue.jumpTo(2))
        assertEquals("c", queue.current)
        assertFalse(queue.jumpTo(3))
        assertFalse(queue.advance())
        assertFalse(queue.isFinished)
    }
}
