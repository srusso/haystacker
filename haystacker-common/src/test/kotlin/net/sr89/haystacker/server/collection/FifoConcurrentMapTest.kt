package net.sr89.haystacker.server.collection

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class FifoConcurrentMapTest {

    @Test
    internal fun putAndRetrieveElements() {
        val map = FifoConcurrentMap<Int, String>(3)

        assertNull(map.get(10))

        map.put(10, "ten")
        map.put(11, "eleven")
        map.put(12, "twelve")

        assertEquals(map.get(10), "ten")
        assertEquals(map.get(11), "eleven")
        assertEquals(map.get(12), "twelve")

        map.put(2, "two")
        map.put(3, "three")

        assertNull(map.get(10))
        assertNull(map.get(11))
        assertEquals(map.get(12), "twelve")
        assertEquals(map.get(2), "two")
        assertEquals(map.get(3), "three")
    }
}