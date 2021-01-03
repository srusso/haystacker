package net.sr89.haystacker.index

import org.junit.jupiter.api.Test

internal class IndexManagerTest {
    val manager: IndexManager = IndexManager()

    @Test
    internal fun createIndex() {
        manager.createIndex("target/lucene-index", "D:\\random")
    }

    @Test
    internal fun searchIndex() {
        manager.searchIndex("target/lucene-index", "path:text")
    }
}