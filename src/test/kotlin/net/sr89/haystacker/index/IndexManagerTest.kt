package net.sr89.haystacker.index

import net.sr89.haystacker.test.common.hasHits
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.LongPoint
import org.apache.lucene.document.TextField
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.Term
import org.apache.lucene.search.TermQuery
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

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
                addDocumentToIndex(it, "myString", 50L)
                addDocumentToIndex(it, "C://path/to/FILE.txt", 50L)
            }

            val foundByString = manager.searchIndex(tempDir.toString(), TermQuery(Term(stringKey, "myString".toLowerCase())))
            val foundByFileName = manager.searchIndex(tempDir.toString(), TermQuery(Term(stringKey, "FILE.txt".toLowerCase())))
            val foundByInPath = manager.searchIndex(tempDir.toString(), TermQuery(Term(stringKey, "to".toLowerCase())))
            val foundByLongRange = manager.searchIndex(tempDir.toString(), LongPoint.newRangeQuery(longKey, 40, 60))
            val foundByLongEquality = manager.searchIndex(tempDir.toString(), LongPoint.newExactQuery(longKey, 50))

            hasHits(foundByString, 1)
            hasHits(foundByFileName, 1)
            hasHits(foundByInPath, 1)
            hasHits(foundByLongRange, 2)
            hasHits(foundByLongEquality, 2)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    internal fun searchIndex() {
        manager.searchIndex("target/lucene-index", TermQuery(Term("path", "IndexManager".toLowerCase())))
    }

    private fun addDocumentToIndex(it: IndexWriter, stringValue: String, longValue: Long) {
        val document = testDocument(stringValue, longValue)
        val documentId = Term(stringKey, stringValue)
        manager.addDocumentToIndex(it, document, documentId)
    }

    private fun testDocument(stringValue: String, longValue: Long): Document {
        val doc = Document()

        doc.add(TextField(stringKey, stringValue, Field.Store.YES))
        doc.add(LongPoint(longKey, longValue))

        return doc
    }
}