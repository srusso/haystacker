package net.sr89.haystacker.index

import mu.KotlinLogging
import net.sr89.haystacker.lang.ast.Symbol
import net.sr89.haystacker.server.async.task.TaskExecutionState.INTERRUPTED
import net.sr89.haystacker.server.async.task.TaskExecutionState.RUNNING
import net.sr89.haystacker.server.async.task.TaskStatus
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.LongPoint
import org.apache.lucene.document.NumericDocValuesField
import org.apache.lucene.document.StoredField
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexableField
import org.apache.lucene.index.Term
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.atomic.AtomicReference

class IndexingFileVisitor(indexPathStr: String, val writer: IndexWriter, val status: AtomicReference<TaskStatus>) :
    SimpleFileVisitor<Path>() {
    private val logger = KotlinLogging.logger {}

    private val indexPath = Paths.get(indexPathStr)
    private var visitedFiles = 0

    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
        return when {
            Files.isReadable(dir) -> {
                FileVisitResult.CONTINUE
            }
            indexPath == dir -> {
                logger.info { "Will not index the index directory itself ($dir)" }
                FileVisitResult.SKIP_SUBTREE
            }
            else -> {
                logger.info { "Skipping unreadable directory $dir" }
                FileVisitResult.SKIP_SUBTREE
            }
        }
    }

    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
        if (status.get().state == INTERRUPTED) {
            return FileVisitResult.TERMINATE
        }

        if (++visitedFiles % 100 == 0) {
            status.set(TaskStatus(RUNNING, "Visiting file or directory #$visitedFiles ($file)"))
        }
        try {
            addFileToIndex(file, attrs)
        } catch (_: IOException) {
            // ignore files that can't be read
        }
        return FileVisitResult.CONTINUE
    }

    override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
        logger.info { "Could not visit $file: ${exc.message}" }
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

    private fun fieldsFor(symbol: Symbol, path: Path, attrs: BasicFileAttributes): List<IndexableField> {
        val creationTime = attrs.creationTime().toMillis()
        val modifiedTime = attrs.lastModifiedTime().toMillis()
        val size = attrs.size()

        return when (symbol) {
            Symbol.NAME -> listOf(
                TextField(Symbol.NAME.luceneQueryName, path.toString(), Field.Store.YES),
                StringField("id", path.toString(), Field.Store.NO)
            )
            Symbol.SIZE -> listOf(
                LongPoint(Symbol.SIZE.luceneQueryName, size),
                NumericDocValuesField(Symbol.SIZE.luceneQueryName, size),
                StoredField(Symbol.SIZE.luceneQueryName, size)
            )
            Symbol.CREATED -> listOf<IndexableField>(
                LongPoint(Symbol.CREATED.luceneQueryName, creationTime),
                NumericDocValuesField(Symbol.CREATED.luceneQueryName, creationTime),
                StoredField(Symbol.CREATED.luceneQueryName, creationTime)
            )
            Symbol.LAST_MODIFIED -> listOf(
                LongPoint(Symbol.LAST_MODIFIED.luceneQueryName, modifiedTime),
                NumericDocValuesField(Symbol.LAST_MODIFIED.luceneQueryName, modifiedTime),
                StoredField(Symbol.LAST_MODIFIED.luceneQueryName, modifiedTime)
            )
        }
    }
}