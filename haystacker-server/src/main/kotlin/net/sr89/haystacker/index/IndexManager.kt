package net.sr89.haystacker.index

import net.sr89.haystacker.async.task.TaskStatus
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.index.Term
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.PrefixQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TopDocs
import org.apache.lucene.store.FSDirectory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.atomic.AtomicReference

interface IndexManager {
    fun createNewIndex(): IndexWriter
    fun openIndex(): IndexWriter
    fun searchIndex(query: Query, maxResults: Int = 5): TopDocs
    fun fetchDocument(docID: Int): Document?
    fun removeDirectoryFromIndex(writer: IndexWriter, path: Path)
    fun indexDirectoryRecursively(writer: IndexWriter, path: Path, status: AtomicReference<TaskStatus>)

    companion object {
        private val managers = hashMapOf<String, IndexManager>()

        @Synchronized
        fun forPath(indexPath: String): IndexManager {
            return managers.computeIfAbsent(indexPath, ::IndexManagerImpl)
        }
    }
}

private class IndexManagerImpl(val indexPath: String) : IndexManager {
    val analyzer: Analyzer = StandardAnalyzer()

    var indexDirectory: FSDirectory? = null
    var reader: IndexReader? = null
    var searcher: IndexSearcher? = null

    override fun createNewIndex(): IndexWriter {
        val iwc = IndexWriterConfig(analyzer)
        iwc.openMode = OpenMode.CREATE

        return IndexWriter(initIndexDirectory(), iwc)
    }

    override fun openIndex(): IndexWriter {
        val iwc = IndexWriterConfig(analyzer)
        iwc.openMode = OpenMode.APPEND

        return IndexWriter(initIndexDirectory(), iwc)
    }

    override fun searchIndex(query: Query, maxResults: Int): TopDocs {
        initSearcher()

        return searcher!!.search(query, maxResults)
    }

    override fun fetchDocument(docID: Int): Document? {
        initSearcher()

        return searcher!!.doc(docID)
    }

    private fun initSearcher() {
        if (searcher == null) {
            reader = DirectoryReader.open(initIndexDirectory())
            searcher = IndexSearcher(reader)
        }
    }

    override fun removeDirectoryFromIndex(writer: IndexWriter, path: Path) {
        writer.deleteDocuments(PrefixQuery(Term("id", path.toString())))
    }

    override fun indexDirectoryRecursively(writer: IndexWriter, path: Path, status: AtomicReference<TaskStatus>) {
        val visitor = IndexingFileVisitor(indexPath, writer, status)
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, visitor)
        } else {
            visitor.addFileToIndex(path, Files.readAttributes(path, BasicFileAttributes::class.java))
        }

        println("Done indexing $path")
    }

    private fun initIndexDirectory(): FSDirectory {
        if (indexDirectory == null) {
            indexDirectory = FSDirectory.open(Paths.get(indexPath))
        }
        return indexDirectory!!
    }
}
