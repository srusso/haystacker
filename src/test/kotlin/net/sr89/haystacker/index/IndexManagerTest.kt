package net.sr89.haystacker.index

import net.sr89.haystacker.test.common.hasHits
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.LongPoint
import org.apache.lucene.document.TextField
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause.Occur.MUST
import org.apache.lucene.search.BooleanClause.Occur.MUST_NOT
import org.apache.lucene.search.BooleanClause.Occur.SHOULD
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.TermQuery
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

internal class IndexManagerTest {
    val stringKey = "stringValue"
    val longKey = "longValue"

    @Test
    internal fun indexDirectory() {
        val manager = IndexManager("target/lucene-index")

        manager.createNewIndex().use { writer ->
            manager.indexDirectoryRecursively(writer, Paths.get("./"))
        }
    }

    @Test
    internal fun indexDocumentsAndSearch() {
        val tempDir = Files.createTempDirectory("tdir").toFile()
        val manager = IndexManager(tempDir.toString())

        try {
            manager.createNewIndex().use {
                addDocumentToIndex(manager, it, "myString", 50L)
                addDocumentToIndex(manager, it, "C://path/to/FILE.txt", 50L)
            }

            val foundByString = manager.searchIndex(TermQuery(Term(stringKey, "myString".toLowerCase())))
            val foundByFileName = manager.searchIndex(TermQuery(Term(stringKey, "FILE.txt".toLowerCase())))
            val foundByInPath = manager.searchIndex(TermQuery(Term(stringKey, "to".toLowerCase())))
            val foundByLongRange = manager.searchIndex(LongPoint.newRangeQuery(longKey, 40, 60))
            val foundByLongEquality = manager.searchIndex(LongPoint.newExactQuery(longKey, 50))

            hasHits(manager, foundByString, 1)
                .includingDocumentWith(Term(stringKey, "myString"))
            hasHits(manager, foundByFileName, 1)
                .includingDocumentWith(Term(stringKey, "C://path/to/FILE.txt"))
            hasHits(manager, foundByInPath, 1)
            hasHits(manager, foundByLongRange, 2)
                .includingDocumentWith(Term(stringKey, "myString"))
                .includingDocumentWith(Term(stringKey, "C://path/to/FILE.txt"))
            hasHits(manager, foundByLongEquality, 2)
                .includingDocumentWith(Term(stringKey, "myString"))
                .includingDocumentWith(Term(stringKey, "C://path/to/FILE.txt"))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    internal fun booleanQueries() {
        val tempDir = Files.createTempDirectory("tdir").toFile()
        val manager = IndexManager(tempDir.toString())

        try {
            manager.createNewIndex().use {
                addDocumentToIndex(manager, it, "myString", 50L)
                addDocumentToIndex(manager, it, "C://path/to/FILE.txt", 50L)
            }

            val findFirst = TermQuery(Term(stringKey, "myString".toLowerCase()))
            val findSecond = TermQuery(Term(stringKey, "FILE.txt".toLowerCase()))
            val findBoth = LongPoint.newRangeQuery(longKey, 40, 60)

            val firstOrSecond = BooleanQuery.Builder().add(findFirst, SHOULD).add(findSecond, SHOULD).build()
            val firstAndSecond = BooleanQuery.Builder().add(findFirst, MUST).add(findSecond, MUST).build()
            val bothButNotFirst = BooleanQuery.Builder().add(findBoth, MUST).add(findFirst, MUST_NOT).build()

            hasHits(manager, manager.searchIndex(firstOrSecond), 2)
                .includingDocumentWith(Term(stringKey, "myString"))
                .includingDocumentWith(Term(stringKey, "C://path/to/FILE.txt"))

            hasHits(manager, manager.searchIndex(firstAndSecond), 0)

            hasHits(manager, manager.searchIndex(bothButNotFirst), 1)
                .includingDocumentWith(Term(stringKey, "C://path/to/FILE.txt"))

        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun addDocumentToIndex(manager: IndexManager, it: IndexWriter, stringValue: String, longValue: Long) {
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