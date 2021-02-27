package net.sr89.haystacker.server.collection

import java.util.LinkedList

/**
 * A FIFO data structure that keeps at most [maxSize] elements,
 * pruning from the head (oldest).
 */
class CircularQueue<T>(private val maxSize: Int): Iterable<T> {
    private val values: LinkedList<T> = LinkedList()

    fun add(value: T) {
        if (values.size >= maxSize) {
            values.removeFirst()
        }

        values.add(value)
    }

    fun get(i: Int): T {
        return values[i]
    }

    fun size(): Int {
        return values.size
    }

    override fun iterator(): Iterator<T> {
        return values.iterator()
    }
}