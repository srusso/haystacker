package net.sr89.haystacker.index

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.LongPoint
import org.apache.lucene.document.TextField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.index.Term
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.Query
import org.apache.lucene.search.TopDocs
import org.apache.lucene.store.FSDirectory
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

class IndexManager(val indexPath: String) {
    val analyzer: Analyzer = StandardAnalyzer()

    var indexDirectory: FSDirectory? = null
    var reader: IndexReader? = null
    var searcher: IndexSearcher? = null

    fun createNewIndex(): IndexWriter {
        val iwc = IndexWriterConfig(analyzer)
        iwc.openMode = OpenMode.CREATE

        return IndexWriter(initIndexDirectory(), iwc)
    }

    fun openIndex(): IndexWriter {
        val iwc = IndexWriterConfig(analyzer)
        iwc.openMode = OpenMode.APPEND

        return IndexWriter(initIndexDirectory(), iwc)
    }

    fun addDocumentToIndex(writer: IndexWriter, document: Document, documentId: Term) {
        if (writer.config.openMode == OpenMode.CREATE) {
            // New index, so we just add the document (no old document can be there):
            writer.addDocument(document)
        } else {
            // Existing index (an old copy of this document may have been indexed) so
            // we use updateDocument instead to replace the old one matching the exact
            // path, if present:
            writer.updateDocument(documentId, document)
        }
    }

    fun searchIndex(query: Query): TopDocs {
        initSearcher()

        return searcher!!.search(query, 5)
    }

    fun fetchDocument(docID: Int): Document? {
        initSearcher()

        return searcher!!.doc(docID)
    }

    private fun initSearcher() {
        if (searcher == null) {
            reader = DirectoryReader.open(initIndexDirectory())
            searcher = IndexSearcher(reader)
        }
    }

    fun indexDirectoryRecursively(writer: IndexWriter, path: Path) {
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    try {
                        val document = createDocumentForFile(file, attrs.lastModifiedTime().toMillis())
                        val documentId = Term("path", file.toString())
                        addDocumentToIndex(writer, document, documentId)
                    } catch (ignore: IOException) {
                        // don't index files that can't be read.
                    }
                    return FileVisitResult.CONTINUE
                }
            })
        } else {
            val document = createDocumentForFile(path, Files.getLastModifiedTime(path).toMillis())
            val documentId = Term("path", path.toString())
            addDocumentToIndex(writer, document, documentId)
        }
    }

    private fun initIndexDirectory(): FSDirectory {
        if (indexDirectory == null) {
            indexDirectory = FSDirectory.open(Paths.get(indexPath))
        }
        return indexDirectory!!
    }

    private fun createDocumentForFile(file: Path, lastModified: Long): Document {
        val doc = Document()

        val pathField: Field = TextField("path", file.toString(), Field.Store.YES)
        doc.add(pathField)

        doc.add(LongPoint("modified", lastModified))

        return doc
    }
}
