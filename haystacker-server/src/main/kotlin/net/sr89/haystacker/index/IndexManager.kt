package net.sr89.haystacker.index

import com.sun.jna.platform.FileMonitor
import net.sr89.haystacker.async.task.BackgroundTaskManager
import net.sr89.haystacker.async.task.TaskStatus
import net.sr89.haystacker.filesystem.IndexUpdatingListener
import net.sr89.haystacker.server.config.SettingsManager
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
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

class IndexManagerProvider(private val settings: SettingsManager, private val taskManager: BackgroundTaskManager) {
    private val managers = hashMapOf<String, IndexManager>()

    @Synchronized
    fun forPath(indexPath: String): IndexManager {
        return managers.computeIfAbsent(indexPath) { p -> IndexManagerImpl(settings, taskManager, p) }
    }
}

data class IndexSearchResult(
    val totalResults: Long,
    val returnedResults: Int,
    val results: List<Document>
)

interface IndexManager {
    /**
     * Creates the index on the hard drive.
     */
    fun createNewIndex()

    /**
     * Search the index.
     */
    fun search(query: Query, maxResults: Int = 5): IndexSearchResult

    /**
     * Remove a directory from the index, recursively. Do not remove the directory itself.
     */
    fun removeDirectory(path: Path, updateListOfIndexedDirectories: Boolean)

    /**
     * Add a new directory and all its contents, recursively, to the index.
     */
    fun addNewDirectory(path: Path, status: AtomicReference<TaskStatus>, updateListOfIndexedDirectories: Boolean)

    /**
     * Returns a list of [Path]s currently indexed by this [IndexManager].
     * Useful to know which file system changes need to be taken into consideration for the purposes of updating the index.
     *
     * For example if:
     *   * directory `C:\MyDirectory` is returned by this method, and
     *   * file `C:\MyDirectory\MyNewFile.txt` is created
     *
     * Then we know that we need to add this file to the index.
     */
    fun indexedDirectories(): Set<Path>

    /**
     * Returns a list of [Path]s ignored by this [IndexManager].
     * Useful to know which file system changes need to be ignored.
     *
     * For example if:
     *   * directory `C:\MyDirectory` is returned by [indexedDirectories], and
     *   * directory `C:\MyDirectory\MySubDirectory` is returned by this method, and
     *   * file `C:\MyDirectory\MySubDirectory\MyNewFile.txt` is created
     *
     * Then we know we should ignore this file.
     *
     * @see [indexedDirectories]
     */
    fun excludedDirectories(): Set<Path>

    fun stopWatchingFileSystemChanges()

    fun startWatchingFileSystemChanges()

    fun startWatching(directory: Path)
}

private val indexedRootDirectoriesId = Term("indexedRootDirectories")
private val excludedRootSubDirectoriesId = Term("excludedRootSubDirectories")

private const val setTermValueDelimiter = ",~#~,"
private const val setTermIdValue = "true"

private class IndexManagerImpl(private val settings: SettingsManager,
                               private val taskManager: BackgroundTaskManager,
                               private val indexPath: String) : IndexManager {

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

    override fun search(query: Query, maxResults: Int): IndexSearchResult {
        initSearcher()

        val hits = searcher.search(query, maxResults)

        val foundDocuments = hits.scoreDocs.map(ScoreDoc::doc).map(::fetchDocument).toList()

        return IndexSearchResult(hits.totalHits.value, hits.scoreDocs.size, foundDocuments)
    }

    override fun removeDirectory(path: Path, updateListOfIndexedDirectories: Boolean) {
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

    override fun addNewDirectory(path: Path, status: AtomicReference<TaskStatus>, updateListOfIndexedDirectories: Boolean) {
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
            startWatching(path)
        }
    }

    override fun indexedDirectories() =
        getSetValue(indexedRootDirectoriesId).second.map { dir -> Paths.get(dir) }.toSet()

    override fun excludedDirectories() =
        getSetValue(excludedRootSubDirectoriesId).second.map { dir -> Paths.get(dir) }.toSet()

    private val watchedDirectories: MutableSet<File> = mutableSetOf()
    private val listeners: MutableSet<IndexUpdatingListener> = mutableSetOf()
    private val observedEvents = FileMonitor.FILE_CREATED or FileMonitor.FILE_DELETED or FileMonitor.FILE_RENAMED or FileMonitor.FILE_SIZE_CHANGED
    private val fileMonitor = FileMonitor.getInstance()

    override fun stopWatchingFileSystemChanges() {
        watchedDirectories.forEach(fileMonitor::removeWatch)
        listeners.forEach(fileMonitor::removeFileListener)
    }

    override fun startWatchingFileSystemChanges() {
        registerFSEventListener()

        for (indexPath in settings.indexes()) {
            for (indexedDirectory in indexedDirectories()) {
                println("Watching $indexedDirectory")
                startWatching(indexedDirectory)
            }
        }
    }

    override fun startWatching(directory: Path) {
        val directoryFile = directory.toFile()

        watchedDirectories.add(directoryFile)
        fileMonitor.addWatch(directoryFile, observedEvents)
    }

    private fun registerFSEventListener() {
        val listener = IndexUpdatingListener(this, taskManager)

        fileMonitor.addFileListener(listener)
        listeners.add(listener)
    }

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

        // "set" documents are documents with two fields:
        //   * A text field containing a list of separated values
        //   * An "ID" field with a similar key as the data field, with a value of "true". Simply used to find the document.
        document.removeFields(term.field())
        document.add(TextField(term.field(), updateSet(dirs).joinToString(setTermValueDelimiter), Field.Store.YES))
        document.add(TextField(idFieldForSetTerm, setTermIdValue, Field.Store.NO))

        this.updateDocument(Term(idFieldForSetTerm, setTermIdValue), document)
    }

    private fun getSetValue(term: Term): Pair<Document, Set<String>> {
        val hits = search(TermQuery(Term(idTermForSetDocument(term), setTermIdValue)))

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
