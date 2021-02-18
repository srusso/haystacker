package net.sr89.haystacker.index

import net.sr89.haystacker.lang.ast.Symbol
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.LongPoint
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.Term
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

class IndexingFileVisitor(val indexPathStr: String, val writer: IndexWriter) : SimpleFileVisitor<Path>() {
    private val indexPath = Paths.get(indexPathStr)
    private var visitedFiles = 0

    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
        return when {
            Files.isReadable(dir) -> {
                FileVisitResult.CONTINUE
            }
            indexPath == dir -> {
                println("Will not index the index directory itself ($dir)")
                FileVisitResult.SKIP_SUBTREE
            }
            else -> {
                println("Skipping unreadable directory $dir")
                FileVisitResult.SKIP_SUBTREE
            }
        }
    }

    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
        if (++visitedFiles % 10000 == 0) {
            println("Visiting file or directory #$visitedFiles ($file)")
        }
        try {
            addFileToIndex(file, attrs)
        } catch (_: IOException) {
            // ignore files that can't be read
        }
        return FileVisitResult.CONTINUE
    }

    override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
        println("Could not visit $file: ${exc.message}")
        return FileVisitResult.CONTINUE
    }

    fun addFileToIndex(file: Path, attrs: BasicFileAttributes) {
        val document = createDocumentForFile(file, attrs)
        val documentId = Term("id", file.toString())
        addDocumentToIndex(document, documentId)
    }

    private fun addDocumentToIndex(document: Document, documentId: Term) {
        writer.updateDocument(documentId, document)
    }

    private fun createDocumentForFile(path: Path, attrs: BasicFileAttributes): Document {
        val doc = Document()

        Symbol.values()
            .flatMap { symbol: Symbol -> fieldsFor(symbol, path, attrs) }
            .forEach(doc::add)

        return doc
    }

    private fun fieldsFor(symbol: Symbol, path: Path, attrs: BasicFileAttributes): List<Field> {
        return when (symbol) {
            Symbol.NAME -> listOf(
                TextField(Symbol.NAME.luceneQueryName, path.toString(), Field.Store.YES),
                StringField("id", path.toString(), Field.Store.NO)
            )
            Symbol.SIZE -> listOf(LongPoint(Symbol.SIZE.luceneQueryName, attrs.size()))
            Symbol.CREATED -> listOf(LongPoint(Symbol.CREATED.luceneQueryName, attrs.creationTime().toMillis()))
            Symbol.LAST_MODIFIED -> listOf(LongPoint(Symbol.LAST_MODIFIED.luceneQueryName, attrs.lastModifiedTime().toMillis()))
        }
    }
}