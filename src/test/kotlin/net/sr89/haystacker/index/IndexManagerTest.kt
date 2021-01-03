package net.sr89.haystacker.index

import org.apache.lucene.demo.IndexFiles
import org.apache.lucene.demo.SearchFiles
import org.junit.jupiter.api.Test

internal class IndexManagerTest {
    @Test
    internal fun createIndex() {
        IndexFiles.main(arrayOf("-index", "target/lucene-index", "-docs", "D:\\random", "-update"))
    }

    @Test
    internal fun searchIndex() {
        SearchFiles.main(arrayOf("-index", "target/lucene-index", "-query", "some"))
    }
}