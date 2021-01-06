package net.sr89.haystacker.index

import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.LongPoint
import org.apache.lucene.document.TextField
import org.apache.lucene.index.Term
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

internal class IndexManagerTest {
    val manager: IndexManager = IndexManager()

    @Test
    internal fun indexDirectory() {
        manager.createIndexWriter("target/lucene-index").use { writer ->
            manager.indexDirectoryRecursively(writer, Paths.get("./"))
        }
    }

    @Test
    internal fun indexManyDocuments() {
        val tempDir = Files.createTempDirectory("tdir")

        var fileToFind: String? = null
        var pathToFind: Path? = null

        val writer = manager.createIndexWriter(tempDir.toString())

        for (i in 0..10) {
            val directory = randomPath(10)
            for (j in 0..10) {
                val fileName = randomString(10)
                val path = Path.of(directory.toString(), fileName)
                val document = testDocument(path.toString(), (i * j).toLong())
                val documentId = Term("path", path.toString())
                manager.addDocumentToIndex(writer, document, documentId)

                if (i * j == 40) {
                    fileToFind = fileName
                    pathToFind = path
                }
            }
        }

        writer.close()

        manager.searchIndex(tempDir.toString(), "path:${fileToFind!!}")
        manager.searchIndex(tempDir.toString(), "path:${pathToFind!!.parent.parent.parent.fileName}")

        Files.delete(Paths.get(tempDir.toUri()))
    }

    @Test
    internal fun searchIndex() {
        manager.searchIndex("target/lucene-index", "path:IndexManager")
    }

    private fun testDocument(path: String, number: Long): Document {
        val doc = Document()

        doc.add(TextField("path", path, Field.Store.YES))
        doc.add(LongPoint("modified", number))

        return doc
    }

    fun randomPath(length: Int): Path {
        val pathParts = listOf("part", "directory", "orange", "random", "icecream", "files", "mystuff", "things", "items", "objects")

        return (1..length)
            .map { pathParts.random() }
            .fold(Paths.get("")) { currPath, next ->
                Path.of(currPath.toString(), next)
            }
    }

    fun randomString(length: Int): String {
        val charset = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { charset.random() }
            .joinToString("")
    }
}