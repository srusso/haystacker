package net.sr89.haystacker.server.collection

class FifoConcurrentMap<K, V>(maxSize: Int) {
    private val keyFifo = CircularQueue<K>(maxSize)
    private val map = HashMap<K, V>()

    @Synchronized
    fun put(key: K, value: V) {
        val removedKey = keyFifo.add(key)
        if (removedKey != null) {
            map.remove(removedKey)
        }
        map[key] = value
    }

    @Synchronized
    fun get(key: K): V? {
        return map[key]
    }
}