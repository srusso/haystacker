package net.sr89.haystacker.server.collection

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CircularQueueTest {

    @Test
    fun emptyQueue() {
        val queue = CircularQueue<Int>(3)

        assertEquals(0, queue.size())
    }

    @Test
    fun notFullQueue() {
        val queue = CircularQueue<Int>(3)

        queue.add(5)
        queue.add(10)

        assertEquals(2, queue.size())
        assertEquals(5, queue.get(0))
        assertEquals(10, queue.get(1))
    }

    @Test
    fun maximumSizeIsNotExceeded() {
        val queue = CircularQueue<Int>(3)

        queue.add(1)
        queue.add(5)
        queue.add(10)
        queue.add(15)
        queue.add(20)

        assertEquals(3, queue.size())
        assertEquals(10, queue.get(0))
        assertEquals(15, queue.get(1))
        assertEquals(20, queue.get(2))
    }
}