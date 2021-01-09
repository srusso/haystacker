package net.sr89.haystacker.index

import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.LongPoint
import org.apache.lucene.document.TextField
import org.apache.lucene.index.Term
import org.apache.lucene.search.TermQuery
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertTrue

internal class IndexManagerTest {
    val stringKey = "stringValue"
    val longKey = "longValue"

    val manager: IndexManager = IndexManager()

    @Test
    internal fun indexDirectory() {
        manager.createIndexWriter("target/lucene-index").use { writer ->
            manager.indexDirectoryRecursively(writer, Paths.get("./"))
        }
    }

    @Test
    internal fun indexDocumentsAndSearch() {
        val tempDir = Files.createTempDirectory("tdir").toFile()

        try {
            manager.createIndexWriter(tempDir.toString()).use {
                val document = testDocument("myString", 50L)
                val documentId = Term(stringKey, "myString")
                manager.addDocumentToIndex(it, document, documentId)
            }

            val foundByString = manager.searchIndex(tempDir.toString(), TermQuery(Term(stringKey, "myString".toLowerCase())))
            val foundByRange = manager.searchIndex(tempDir.toString(), LongPoint.newRangeQuery(longKey, 40, 60))

            assertTrue(foundByString.totalHits.value == 1L, "One document should be found by string term")
            assertTrue(foundByRange.totalHits.value == 1L, "One document should be found by range")
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    internal fun searchIndex() {
        manager.searchIndex("target/lucene-index", TermQuery(Term("path", "IndexManager".toLowerCase())))
    }

    private fun testDocument(stringValue: String, longValue: Long): Document {
        val doc = Document()

        doc.add(TextField(stringKey, stringValue, Field.Store.YES))
        doc.add(LongPoint(longKey, longValue))

        return doc
    }
}