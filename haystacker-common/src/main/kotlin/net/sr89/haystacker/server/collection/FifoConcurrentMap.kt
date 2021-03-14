package net.sr89.haystacker.server.collection

import java.util.concurrent.ConcurrentHashMap

class FifoConcurrentMap<K, V>(maxSize: Int) {
    private val keyFifo = CircularQueue<K>(maxSize)
    private val map = ConcurrentHashMap<K, V>()

    fun put(key: K, value: V) {
        val removedKey = keyFifo.add(key)
        if (removedKey != null) {
            map.remove(removedKey)
        }
        map[key] = value
    }

    fun get(key: K): V? {
        return map[key]
    }
}