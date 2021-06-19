package net.sr89.haystacker.index

import com.sun.jna.platform.FileMonitor
import mu.KotlinLogging
import net.sr89.haystacker.filesystem.IndexUpdatingListener
import net.sr89.haystacker.lang.ToLuceneClauseVisitor
import net.sr89.haystacker.lang.ast.HslQuery
import net.sr89.haystacker.lang.ast.HslSortField
import net.sr89.haystacker.lang.ast.SortOrder
import net.sr89.haystacker.lang.ast.Symbol
import net.sr89.haystacker.server.api.SearchResponse
import net.sr89.haystacker.server.api.SearchResult
import net.sr89.haystacker.server.async.task.BackgroundTaskManager
import net.sr89.haystacker.server.async.task.TaskStatus
import net.sr89.haystacker.server.file.isParentOf
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.TextField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexNotFoundException
import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.index.Term
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.PrefixQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.Sort
import org.apache.lucene.search.SortField
import org.apache.lucene.search.SortField.Type
import org.apache.lucene.search.TermQuery
import org.apache.lucene.store.FSDirectory
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

class IndexManagerProvider(private val taskManager: BackgroundTaskManager) {
    private val managers = hashMapOf<String, IndexManager>()

    @Synchronized
    fun forPath(indexPath: String): IndexManager {
        return managers.computeIfAbsent(indexPath) { p -> IndexManagerImpl(taskManager, p) }
    }

    fun getAll(): Set<IndexManager> {
        return managers.values.toSet()
    }
}

private data class LuceneSearchResult(
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
    fun search(query: HslQuery, maxResults: Int = 5): SearchResponse

    /**
     * Remove a directory from the index, recursively. Do not remove the directory itself.
     */
    fun removeDirectory(path: Path, updateListOfIndexedDirectories: Boolean)

    /**
     * Add a new directory and all its contents, recursively, to the index.
     */
    fun addNewDirectory(path: Path, status: AtomicReference<TaskStatus>, startWatchingDirectory: Boolean)

    /**
     * Returns true if the file or directory specified by [file] is of interest for this index.
     *
     * For example if:
     *   * directory `C:\MyDirectory` is indexed by this manager, and
     *   * its subdirectory `C:\MyDirectory\MySubDirectory` is excluded from the index
     *
     * Then, calling this method for:
     *
     *   * `C:\MyDirectory\MyNewFile.txt`, will return `true`
     *   * `C:\MyDirectory\MySubDirectory\MyNewFile.txt`, will return `false`
     */
    fun fileIsRelevantForIndex(file: File): Boolean

    /**
     * Stops observing changes to the file system.
     * To be used when shutting down the server.
     */
    fun stopWatchingFileSystemChanges()

    /**
     * Start observing changes to the directories that belong to this index, in order to keep the index up to date in almost real time.
     */
    fun startWatchingFileSystemChanges()

    fun onVolumeMounted(volume: Path)

    fun onVolumeUnmounted(volume: Path)

}

private val indexedRootDirectoriesId = Term("indexedRootDirectories")
private val excludedRootSubDirectoriesId = Term("excludedRootSubDirectories")

private const val setTermValueDelimiter = ",~#~,"
private const val setTermIdValue = "true"

private const val observedEvents =
    FileMonitor.FILE_CREATED or FileMonitor.FILE_DELETED or FileMonitor.FILE_RENAMED or FileMonitor.FILE_SIZE_CHANGED
private val fileMonitor = FileMonitor.getInstance()

private fun toSearchResult(document: Document): SearchResult {
    return SearchResult(
        document.getField(Symbol.NAME.luceneQueryName).stringValue(),
        numericFieldOrZero(document, Symbol.SIZE.luceneQueryName),
        numericFieldOrZero(document, Symbol.CREATED.luceneQueryName),
        numericFieldOrZero(document, Symbol.LAST_MODIFIED.luceneQueryName)
    )
}

private fun numericFieldOrZero(document: Document, fieldName: String) =
    document.getField(fieldName)?.numericValue()?.toLong() ?: 0L

internal class IndexManagerImpl(
    taskManager: BackgroundTaskManager,
    private val indexPath: String
) : IndexManager {
    private val logger = KotlinLogging.logger {}

    private val analyzer: Analyzer = StandardAnalyzer()

    private var indexDirectory: FSDirectory? = null
    private lateinit var reader: IndexReader
    private lateinit var searcher: IndexSearcher
    private val indexLock = ReentrantReadWriteLock()

    private val watchedDirectories: MutableSet<File> = mutableSetOf()
    private val fileSystemListener = IndexUpdatingListener(this, taskManager)

    override fun createNewIndex() {
        val iwc = IndexWriterConfig(analyzer)
        iwc.openMode = OpenMode.CREATE

        // creates the index
        IndexWriter(initIndexDirectory(true), iwc).close()
    }

    override fun search(query: HslQuery, maxResults: Int): SearchResponse {
        val luceneQuery = query.clause.accept(ToLuceneClauseVisitor(analyzer))

        val luceneSearchResult = if (query.sort.sortFields.isEmpty()) {
            lowLevelLuceneSearch(luceneQuery, maxResults)
        } else {
            val luceneSort = Sort(*query.sort.sortFields.map(this::toLuceneSortField).toTypedArray())
            lowLevelLuceneSearch(luceneQuery, maxResults, luceneSort)
        }

        return SearchResponse(
            luceneSearchResult.totalResults,
            luceneSearchResult.returnedResults,
            luceneSearchResult.results.map(::toSearchResult)
        )
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

    override fun addNewDirectory(path: Path, status: AtomicReference<TaskStatus>, startWatchingDirectory: Boolean) {
        val addDirectoryToWatchedList = startWatchingDirectory && Files.isDirectory(path)

        indexLock.write {
            newIndexWriter().use {
                if (addDirectoryToWatchedList) {
                    it.addToSetTerm(indexedRootDirectoriesId, path.toString())
                    it.removeFromSetTerm(excludedRootSubDirectoriesId, path.toString())
                }

                updateFileOrDirectoryInIndex(it, path, status)
            }
        }

        logger.info { "Done indexing $path" }

        if (addDirectoryToWatchedList) {
            startWatching(path)
        }
    }

    override fun stopWatchingFileSystemChanges() {
        watchedDirectories.forEach(fileMonitor::removeWatch)
        fileMonitor.removeFileListener(fileSystemListener)
    }

    override fun startWatchingFileSystemChanges() {
        try {
            val indexedDirectories = indexedDirectories()

            fileMonitor.addFileListener(fileSystemListener)

            for (indexedDirectory in indexedDirectories) {
                startWatching(indexedDirectory)
            }
        } catch (e: IndexNotFoundException) {
            logger.info { "WARN: Ignoring missing index $indexPath" }
        }
    }

    override fun onVolumeMounted(volume: Path) {
        watchedDirectories
            .map(File::toPath)
            .filter { watchedDirectory -> volume.isParentOf(watchedDirectory) }
            .forEach(this::setWatch)
    }

    override fun onVolumeUnmounted(volume: Path) {
        watchedDirectories
            .map(File::toPath)
            .filter { watchedDirectory -> volume.isParentOf(watchedDirectory) }
            .forEach(this::stopWatch)
    }

    override fun fileIsRelevantForIndex(file: File): Boolean {
        val filePath: Path = file.toPath()
        return indexedDirectories()
            .any { indexedDirectory -> indexedDirectory.isParentOf(filePath) }
            .and(excludedDirectories()
                .none { excludedDirectory -> excludedDirectory.isParentOf(filePath) })
    }

    private fun lowLevelLuceneSearch(query: Query, maxResults: Int = 5, sort: Sort? = null): LuceneSearchResult {
        initSearcher()

        val hits = if (sort == null) {
            searcher.search(query, maxResults)
        } else {
            searcher.search(query, maxResults, sort)
        }

        val foundDocuments = hits.scoreDocs.map(ScoreDoc::doc).map(::fetchDocument).toList()

        return LuceneSearchResult(hits.totalHits.value, hits.scoreDocs.size, foundDocuments)
    }

    private fun toLuceneSortField(hslSortField: HslSortField): SortField {
        val reverse = when (hslSortField.sortOrder) {
            SortOrder.ASCENDING -> false
            SortOrder.DESCENDING -> true
        }

        // TODO remove hardcoded Type.LONG from here
        return SortField(hslSortField.field.luceneQueryName, Type.LONG, reverse)
    }

    private fun indexedDirectories() =
        getSetValue(indexedRootDirectoriesId).second.map { dir -> Paths.get(dir) }.toSet()

    private fun excludedDirectories() =
        getSetValue(excludedRootSubDirectoriesId).second.map { dir -> Paths.get(dir) }.toSet()

    private fun startWatching(directory: Path) {
        watchedDirectories.add(directory.toFile())
        setWatch(directory)
    }

    private fun setWatch(directory: Path) {
        try {
            fileMonitor.addWatch(directory.toFile(), observedEvents)
            logger.info { "Watching $directory" }
        } catch (e: FileNotFoundException) {
            logger.info { "Not watching $directory (indexed in $indexPath) because it's not currently mounted" }
        }
    }

    private fun stopWatch(directory: Path) {
        logger.info { "Stopped watching $directory" }
        fileMonitor.removeWatch(directory.toFile())
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
        val hits = lowLevelLuceneSearch(TermQuery(Term(idTermForSetDocument(term), setTermIdValue)))

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

        return IndexWriter(initIndexDirectory(false), iwc)
    }

    private fun initIndexDirectory(creation: Boolean): FSDirectory {
        val path = Paths.get(indexPath)

        if (!creation && !Files.exists(path)) {
            throw IndexNotFoundException("Unable to find index at $path")
        }

        if (indexDirectory == null) {
            indexDirectory = FSDirectory.open(path)
        }
        return indexDirectory!!
    }

    @Throws(IndexNotFoundException::class)
    private fun initSearcher() {
        // Probably fine to initialize this once per search.
        // If performance concerns arise, we should look into this.
        reader = DirectoryReader.open(initIndexDirectory(false))
        searcher = IndexSearcher(reader)
    }
}
