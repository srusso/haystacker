package net.sr89.haystacker.index

import net.sr89.haystacker.lang.parser.HslParser
import net.sr89.haystacker.server.async.task.TaskExecutionState.NOT_STARTED
import net.sr89.haystacker.server.async.task.TaskStatus
import net.sr89.haystacker.test.common.SingleThreadTaskManager
import net.sr89.haystacker.test.common.assertSortedSearchResult
import net.sr89.haystacker.test.common.setTimes
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

internal class IndexManagerImplTest {

    val oldFile = "aaa.txt"
    val olderFileModifiedRecently = "zzz.txt"
    val midFile = "bbb.txt"
    val newFile = "ccc.txt"

    lateinit var indexFile: Path
    lateinit var directoryToIndex: Path

    lateinit var manager: IndexManagerImpl


    @BeforeEach
    internal fun setUp() {
        directoryToIndex = Files.createTempDirectory("files")
        indexFile = Files.createTempDirectory("index")

        createFiles()

        manager = IndexManagerImpl(
            SingleThreadTaskManager(),
            indexFile.toString()
        )

        manager.createNewIndex()
    }

    @Test
    internal fun indexAndSearchFiles() {
        manager.addNewDirectory(directoryToIndex, AtomicReference(TaskStatus(NOT_STARTED, "")), false)

        assertSortedSearchResult(searchThreeFiles("ORDER BY last_modified ASC"), listOf(oldFile, midFile, newFile))
        assertSortedSearchResult(searchThreeFiles("ORDER BY last_modified DESC"), listOf(newFile, midFile, oldFile))

        assertSortedSearchResult(searchThreeFiles("ORDER BY created ASC"), listOf(oldFile, midFile, newFile))
        assertSortedSearchResult(searchThreeFiles("ORDER BY created DESC"), listOf(newFile, midFile, oldFile))

        assertSortedSearchResult(searchThreeFiles("ORDER BY size ASC"), listOf(newFile, midFile, oldFile))
        assertSortedSearchResult(searchThreeFiles("ORDER BY size DESC"), listOf(oldFile, midFile, newFile))

        assertSortedSearchResult(
            searchAllFiles("ORDER BY created ASC, last_modified ASC"),
            listOf(olderFileModifiedRecently, oldFile, midFile, newFile))

        assertSortedSearchResult(
            searchAllFiles("ORDER BY size DESC, last_modified ASC, created ASC"),
            listOf(oldFile, olderFileModifiedRecently, midFile, newFile))

        assertSortedSearchResult(
            searchAllFiles("ORDER BY size ASC, last_modified ASC, created ASC"),
            listOf(newFile, midFile, oldFile, olderFileModifiedRecently))
    }

    private fun searchThreeFiles(orderByClause: String) = manager.search(
        HslParser().parse("name = $oldFile OR name = $midFile OR name = $newFile $orderByClause")
    )

    private fun searchAllFiles(orderByClause: String) = manager.search(
        HslParser().parse("name = $oldFile OR name = $midFile OR name = $newFile OR name = $olderFileModifiedRecently $orderByClause")
    )

    private fun createFiles() {
        val oldFile = directoryToIndex.resolve(oldFile)
        val olderFileModifiedRecently = directoryToIndex.resolve(olderFileModifiedRecently)
        val midFile = directoryToIndex.resolve(midFile)
        val newFile = directoryToIndex.resolve(newFile)

        Files.newOutputStream(olderFileModifiedRecently).use {
            it.write("Loooooooooooooooooooooooooooong".toByteArray())
        }

        Files.newOutputStream(oldFile).use {
            it.write("Loooooooooooooooooooooooooooong".toByteArray())
        }

        Files.newOutputStream(midFile).use {
            it.write("Meeeeeeeeeeeeeeeedium".toByteArray())
        }

        Files.newOutputStream(newFile).use {
            it.write("Short".toByteArray())
        }

        oldFile.setTimes(Instant.ofEpochMilli(10), Instant.ofEpochMilli(10))
        olderFileModifiedRecently.setTimes(Instant.ofEpochMilli(100), Instant.ofEpochMilli(9))
        midFile.setTimes(Instant.ofEpochMilli(15), Instant.ofEpochMilli(15))
        newFile.setTimes(Instant.ofEpochMilli(20), Instant.ofEpochMilli(20))
    }
}