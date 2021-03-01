package net.sr89.haystacker.index

import net.sr89.haystacker.async.task.TaskStatus
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.TextField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.index.Term
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.PrefixQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TopDocs
import org.apache.lucene.store.FSDirectory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.atomic.AtomicReference

interface IndexManager {
    // TODO remove createNewIndex and openIndex from interface, and remove `writer: IndexWriter` from
    // TODO the methods that accept it as parameter
    fun createNewIndex(): IndexWriter
    fun openIndex(): IndexWriter
    fun searchIndex(query: Query, maxResults: Int = 5): TopDocs
    fun fetchDocument(docID: Int): Document?
    fun removeDirectoryFromIndex(writer: IndexWriter, path: Path, updateListOfIndexedDirectories: Boolean)
    fun addNewDirectoryToIndex(writer: IndexWriter, path: Path, status: AtomicReference<TaskStatus>, updateListOfIndexedDirectories: Boolean)
    fun indexedDirectories(): Set<Path>
    fun excludedDirectories(): Set<Path>
    fun indexPath(): String

    companion object {
        private val managers = hashMapOf<String, IndexManager>()

        @Synchronized
        fun forPath(indexPath: String): IndexManager {
            return managers.computeIfAbsent(indexPath, ::IndexManagerImpl)
        }
    }
}

private class IndexManagerImpl(val indexPath: String) : IndexManager {
    private val indexedRootDirectoriesId = Term("indexedRootDirectories")
    private val excludedRootSubDirectoriesId = Term("excludedRootSubDirectories")

    private val delimiter = ",~#~,"

    private val analyzer: Analyzer = StandardAnalyzer()

    // TODO make these not nullable
    private var indexDirectory: FSDirectory? = null
    private var reader: IndexReader? = null
    private var searcher: IndexSearcher? = null

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

    override fun fetchDocument(docID: Int): Document {
        initSearcher()

        return searcher!!.doc(docID)
    }

    override fun removeDirectoryFromIndex(writer: IndexWriter, path: Path, updateListOfIndexedDirectories: Boolean) {
        if (updateListOfIndexedDirectories) {
            addItemToDelimitedListTerm(excludedRootSubDirectoriesId, path.toString(), writer)
            removeItemFromDelimitedListTerm(indexedRootDirectoriesId, path.toString(), writer)
        }

        writer.deleteDocuments(PrefixQuery(Term("id", path.toString())))
    }

    override fun addNewDirectoryToIndex(writer: IndexWriter, path: Path, status: AtomicReference<TaskStatus>, updateListOfIndexedDirectories: Boolean) {
        if (updateListOfIndexedDirectories) {
            addItemToDelimitedListTerm(indexedRootDirectoriesId, path.toString(), writer)
            removeItemFromDelimitedListTerm(excludedRootSubDirectoriesId, path.toString(), writer)
        }

        updateFileOrDirectoryInIndex(writer, path, status)
    }

    private fun updateFileOrDirectoryInIndex(writer: IndexWriter, path: Path, status: AtomicReference<TaskStatus>) {
        val visitor = IndexingFileVisitor(indexPath, writer, status)
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, visitor)
        } else {
            visitor.addFileToIndex(path, Files.readAttributes(path, BasicFileAttributes::class.java))
        }

        println("Done indexing $path")
    }

    override fun indexedDirectories() =
        searchExistingSeparatedString(indexedRootDirectoriesId).second.map { dir -> Paths.get(dir) }.toSet()

    override fun excludedDirectories() =
        searchExistingSeparatedString(excludedRootSubDirectoriesId).second.map { dir -> Paths.get(dir) }.toSet()

    override fun indexPath(): String {
        return indexPath
    }

    private fun initIndexDirectory(): FSDirectory {
        if (indexDirectory == null) {
            indexDirectory = FSDirectory.open(Paths.get(indexPath))
        }
        return indexDirectory!!
    }

    private fun initSearcher() {
        if (searcher == null) {
            reader = DirectoryReader.open(initIndexDirectory())
            searcher = IndexSearcher(reader)
        }
    }

    private fun addItemToDelimitedListTerm(term: Term, newItem: String, writer: IndexWriter) {
        val (document, dirs) = searchExistingSeparatedString(term)

        document.add(TextField(term.field(), dirs.plus(newItem).joinToString(delimiter), Field.Store.YES))

        writer.updateDocument(term, document)
    }

    private fun removeItemFromDelimitedListTerm(term: Term, newItem: String, writer: IndexWriter) {
        val (document, dirs) = searchExistingSeparatedString(term)

        document.add(TextField(term.field(), dirs.minus(newItem).joinToString(delimiter), Field.Store.YES))

        writer.updateDocument(term, document)
    }

    private fun searchExistingSeparatedString(term: Term): Pair<Document, Set<String>> {
        val dirDoc = searchIndex(TermQuery(term))

        return if (dirDoc.totalHits.value == 1L) {
            val existingDoc = fetchDocument(dirDoc.scoreDocs[0].doc)
            Pair(existingDoc, existingDoc.getField(term.field()).stringValue()!!.split(delimiter).toSet())
        } else {
            Pair(Document(), setOf())
        }
    }
}
