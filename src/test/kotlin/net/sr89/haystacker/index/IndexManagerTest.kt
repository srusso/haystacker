package net.sr89.haystacker.index

import org.junit.jupiter.api.Test
import java.nio.file.Paths

internal class IndexManagerTest {
    val manager: IndexManager = IndexManager()

    @Test
    internal fun indexDirectory() {
        manager.createIndexWriter("target/lucene-index").use {
            writer -> manager.indexDocs(writer, Paths.get("D:\\random"))
        }
    }

    @Test
    internal fun searchIndex() {
        manager.searchIndex("target/lucene-index", "path:text")
    }
}