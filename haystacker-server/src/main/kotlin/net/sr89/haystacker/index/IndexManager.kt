package net.sr89.haystacker.index

import net.sr89.haystacker.async.task.TaskStatus
import net.sr89.haystacker.filesystem.FileSystemWatcher
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
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TermQuery
import org.apache.lucene.store.FSDirectory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

class IndexManagerProvider {
    lateinit var fileSystemWatcher: FileSystemWatcher
    private val managers = hashMapOf<String, IndexManager>()
    private val counter = AtomicLong()

    @Synchronized
    fun forPath(indexPath: String): IndexManager {
        return managers.computeIfAbsent(indexPath) { p -> IndexManagerImpl(counter.getAndIncrement(), fileSystemWatcher, p) }
    }
}

data class IndexSearchResult(
    val totalResults: Long,
    val returnedResults: Int,
    val results: List<Document>
)

interface IndexManager {
    fun createNewIndex()
    fun searchIndex(query: Query, maxResults: Int = 5): IndexSearchResult
    fun removeDirectoryFromIndex(path: Path, updateListOfIndexedDirectories: Boolean)
    fun addNewDirectoryToIndex(path: Path, status: AtomicReference<TaskStatus>, updateListOfIndexedDirectories: Boolean)
    fun indexedDirectories(): Set<Path>
    fun excludedDirectories(): Set<Path>
    fun getUniqueManagerIdentifier(): Long
}

private class IndexManagerImpl(private val id: Long, val fileSystemWatcher: FileSystemWatcher, val indexPath: String) : IndexManager {
    private val indexedRootDirectoriesId = Term("indexedRootDirectories")
    private val excludedRootSubDirectoriesId = Term("excludedRootSubDirectories")

    private val setTermValueDelimiter = ",~#~,"
    private val setTermIdValue = "true"

    private val analyzer: Analyzer = StandardAnalyzer()

    private var indexDirectory: FSDirectory? = null
    private lateinit var reader: IndexReader
    private lateinit var searcher: IndexSearcher
    private val indexLock = ReentrantReadWriteLock()

    override fun createNewIndex() {
        val iwc = IndexWriterConfig(analyzer)
        iwc.openMode = OpenMode.CREATE

        // creates the index
        IndexWriter(initIndexDirectory(), iwc).close()
    }

    override fun searchIndex(query: Query, maxResults: Int): IndexSearchResult {
        initSearcher()

        val hits = searcher.search(query, maxResults)

        val foundDocuments = hits.scoreDocs.map(ScoreDoc::doc).map(::fetchDocument).toList()

        return IndexSearchResult(hits.totalHits.value, hits.scoreDocs.size, foundDocuments)
    }

    override fun removeDirectoryFromIndex(path: Path, updateListOfIndexedDirectories: Boolean) {
        indexLock.write {
            newIndexWriter().use {
                if (updateListOfIndexedDirectories && Files.isDirectory(path)) {
                    it.addToSetTerm(excludedRootSubDirectoriesId, path.toString())
                    it.removeFromSetTerm(indexedRootDirectoriesId, path.toString())
                }

                it.deleteDocuments(PrefixQuery(Term("id", path.toString())))
            }
        }
    }

    override fun addNewDirectoryToIndex(path: Path, status: AtomicReference<TaskStatus>, updateListOfIndexedDirectories: Boolean) {
        val addDirectoryToWatchedList = updateListOfIndexedDirectories && Files.isDirectory(path)

        indexLock.write {
            newIndexWriter().use {
                if (addDirectoryToWatchedList) {
                    it.addToSetTerm(indexedRootDirectoriesId, path.toString())
                    it.removeFromSetTerm(excludedRootSubDirectoriesId, path.toString())
                }

                updateFileOrDirectoryInIndex(it, path, status)
            }
        }

        println("Done indexing $path")

        if (addDirectoryToWatchedList) {
            fileSystemWatcher.startWatching(this, path)
        }
    }

    override fun indexedDirectories() =
        getSetValue(indexedRootDirectoriesId).second.map { dir -> Paths.get(dir) }.toSet()

    override fun excludedDirectories() =
        getSetValue(excludedRootSubDirectoriesId).second.map { dir -> Paths.get(dir) }.toSet()

    override fun getUniqueManagerIdentifier(): Long = id

    private fun fetchDocument(docID: Int): Document {
        return searcher.doc(docID)
    }

    private fun updateFileOrDirectoryInIndex(writer: IndexWriter, path: Path, status: AtomicReference<TaskStatus>) {
        val visitor = IndexingFileVisitor(indexPath, writer, status)
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, visitor)
        } else {
            visitor.addFileToIndex(path, Files.readAttributes(path, BasicFileAttributes::class.java))
        }
    }

    private fun IndexWriter.addToSetTerm(term: Term, newItem: String) =
        updateSetTerm(term) { currentValues -> currentValues.plus(newItem) }

    private fun IndexWriter.removeFromSetTerm(term: Term, toRemove: String) =
        updateSetTerm(term) { currentValues -> currentValues.minus(toRemove) }

    private fun IndexWriter.updateSetTerm(term: Term, updateSet: (Set<String>) -> Set<String>) {
        val (document, dirs) = getSetValue(term)
        val idFieldForSetTerm = idTermForSetDocument(term)

        document.removeFields(term.field())
        document.add(TextField(term.field(), updateSet(dirs).joinToString(setTermValueDelimiter), Field.Store.YES))
        document.add(TextField(idFieldForSetTerm, setTermIdValue, Field.Store.NO))

        this.updateDocument(Term(idFieldForSetTerm, setTermIdValue), document)
    }

    private fun getSetValue(term: Term): Pair<Document, Set<String>> {
        val hits = searchIndex(TermQuery(Term(idTermForSetDocument(term), setTermIdValue)))

        return if (hits.totalResults == 1L) {
            val existingDoc = hits.results[0]
            Pair(existingDoc, existingDoc.getField(term.field()).stringValue()!!.split(setTermValueDelimiter).toSet())
        } else {
            Pair(Document(), setOf())
        }
    }

    private fun idTermForSetDocument(term: Term) = term.field() + "_setid"

    private fun newIndexWriter(): IndexWriter {
        val iwc = IndexWriterConfig(analyzer)
        iwc.openMode = OpenMode.APPEND

        return IndexWriter(initIndexDirectory(), iwc)
    }

    private fun initIndexDirectory(): FSDirectory {
        if (indexDirectory == null) {
            indexDirectory = FSDirectory.open(Paths.get(indexPath))
        }
        return indexDirectory!!
    }

    private fun initSearcher() {
        // Probably fine to initialize this once per search.
        // If performance concerns arise, we should look into this.
        reader = DirectoryReader.open(initIndexDirectory())
        searcher = IndexSearcher(reader)
    }
}
