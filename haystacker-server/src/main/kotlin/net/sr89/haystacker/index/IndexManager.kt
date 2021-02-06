package net.sr89.haystacker.index

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

    fun searchIndex(query: Query, maxResults: Int = 5): TopDocs {
        initSearcher()

        return searcher!!.search(query, maxResults)
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

    fun removeDirectoryFromIndex(writer: IndexWriter, path: Path) {
        writer.deleteDocuments(PrefixQuery(Term("id", path.toString())))
    }

    fun indexDirectoryRecursively(writer: IndexWriter, path: Path) {
        val visitor = IndexingFileVisitor(indexPath, writer)
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
