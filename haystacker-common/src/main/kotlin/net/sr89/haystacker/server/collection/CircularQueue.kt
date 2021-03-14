package net.sr89.haystacker.server.collection

import java.util.LinkedList

/**
 * A FIFO data structure that keeps at most [maxSize] elements,
 * pruning from the head (oldest).
 */
class CircularQueue<T>(private val maxSize: Int) {
    private val values: LinkedList<T> = LinkedList()

    /**
     * Adds a new element to the queue.
     *
     * @return The head, if it had to be removed to make space for the new element. Otherwise, null.
     */
    fun add(value: T): T? {
        val removedElement: T? = if (values.size >= maxSize) {
            values.removeFirst()
        } else {
            null
        }

        values.add(value)

        return removedElement
    }

    fun get(i: Int): T {
        return values[i]
    }

    fun size(): Int {
        return values.size
    }
}