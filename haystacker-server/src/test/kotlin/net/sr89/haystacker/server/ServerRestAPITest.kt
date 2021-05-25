package net.sr89.haystacker.server

import net.sr89.haystacker.server.api.HaystackerRestClient
import net.sr89.haystacker.server.async.task.TaskExecutionState
import net.sr89.haystacker.server.config.ServerConfig
import net.sr89.haystacker.server.filter.ExceptionHandler
import net.sr89.haystacker.server.handlers.HaystackerRoutes
import net.sr89.haystacker.test.common.NoOpServer
import net.sr89.haystacker.test.common.assertSearchResult
import net.sr89.haystacker.test.common.createServerTestFiles
import org.http4k.core.Status
import org.http4k.server.Http4kServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.provider
import org.kodein.di.singleton
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneOffset
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.test.assertEquals

internal class HaystackerApplicationKtTest {
    private val oldInstant: Instant = LocalDate.of(2015, Month.APRIL, 1).atStartOfDay().toInstant(ZoneOffset.UTC)

    lateinit var directoryToIndex: Path
    lateinit var settingsDirectory: Path
    lateinit var subDirectory: Path
    lateinit var indexFile: Path

    lateinit var client: HaystackerRestClient

    @BeforeEach
    internal fun setUp() {
        directoryToIndex = Files.createTempDirectory("files")
        settingsDirectory = Files.createTempDirectory("settings")
        subDirectory = directoryToIndex.resolve("subdirectory")
        indexFile = Files.createTempDirectory("index")

        createServerTestFiles(oldInstant, directoryToIndex, subDirectory)

        val config = ServerConfig(0, Paths.get("."))

        val testOverrides = DI.Module("DITestOverrides") {
            bind<ExecutorService>(overrides = true) with provider { Executors.newSingleThreadExecutor() }
            bind<Http4kServer>(overrides = true) with singleton { NoOpServer() }
        }

        val testDI = DI {
            import(utilModule)
            import(handlersModule)
            import(managerModule(config))

            import(testOverrides, allowOverride = true)
        }

        val myRoutes: HaystackerRoutes by testDI.instance()

        client = HaystackerRestClient("", ExceptionHandler(myRoutes.routesHandler()))

        client.createIndex(indexFile.toAbsolutePath().toString())
        addDirectoryToIndex(indexFile, directoryToIndex)
    }

    @AfterEach
    internal fun tearDown() {
        println("Shutting down server: ${client.shutdownServer().status}")

        directoryToIndex.toFile().deleteRecursively()
        settingsDirectory.toFile().deleteRecursively()
        indexFile.toFile().deleteRecursively()
    }

    @Test
    internal fun searchByFilename() {
        assertSearchResult(client.search(
            "name = oldfile.txt",
            15,
            indexFile.toAbsolutePath().toString()
        ).responseBody(), listOf("oldFile.txt"))

        assertSearchResult(client.search(
            "name = \"subfile.txt\"",
            15,
            indexFile.toAbsolutePath().toString()
        ).responseBody(), listOf("subFile.txt"))

        removeDirectoryFromIndex(subDirectory)

        assertSearchResult(client.search(
            "name = oldfile.txt",
            15,
            indexFile.toAbsolutePath().toString()
        ).responseBody(), listOf("oldFile.txt"))

        assertSearchResult(client.search(
            "name = \"subfile.txt\"",
            15,
            indexFile.toAbsolutePath().toString()
        ).responseBody(), emptyList())
    }

    @Test
    internal fun searchByFilenameIsCaseInsensitive() {
        val search1 = client.search(
                "name = OLDFILE.txt",
                15,
                indexFile.toAbsolutePath().toString()
            ).responseBody()

        val search2 = client.search(
                "name = OlDfIlE.txt",
                15,
                indexFile.toAbsolutePath().toString()
            ).responseBody()

        assertSearchResult(search1, listOf("oldFile.txt"))
        assertSearchResult(search2, listOf("oldFile.txt"))
    }

    @Test
    internal fun searchByFileSize() {
        val searchSmallFiles = client.search(
                "size < 500",
                15,
                indexFile.toAbsolutePath().toString()
            ).responseBody()

        val searchBigFiles = client.search(
                "size > 500",
                15,
                indexFile.toAbsolutePath().toString()
            ).responseBody()

        assertSearchResult(searchBigFiles, listOf("bigbinary.dat"))
        assertSearchResult(searchSmallFiles, listOf("oldFile.txt", "binary.dat", "subFile.txt"))
    }

    @Test
    internal fun searchByLastModifiedTime() {
        val searchOldFiles = client.search(
                "last_modified < 2016-03-01",
                15,
                indexFile.toAbsolutePath().toString()
            ).responseBody()

        val searchNewFiles = client.search(
                "last_modified > 2016-03-01",
                15,
                indexFile.toAbsolutePath().toString()
            ).responseBody()

        assertSearchResult(searchOldFiles, listOf("oldFile.txt"))
        assertSearchResult(searchNewFiles, listOf("bigbinary.dat", "binary.dat", "subFile.txt"))
    }

    @Test
    internal fun searchByCreatedTime() {
        val searchOldFiles = client.search(
                "created < 2016-03-01",
                15,
                indexFile.toAbsolutePath().toString()
            ).responseBody()

        val searchNewFiles = client.search(
                "created > 2016-03-01",
                15,
                indexFile.toAbsolutePath().toString()
            ).responseBody()

        assertSearchResult(searchOldFiles, listOf("oldFile.txt"))
        assertSearchResult(searchNewFiles, listOf("bigbinary.dat", "binary.dat", "subFile.txt"))
    }

    @Test
    internal fun searchWithAndClause() {
        val searchNewAndBigFiles = client.search(
                "created > 2016-03-01 AND size > 500",
                15,
                indexFile.toAbsolutePath().toString()
            ).responseBody()

        assertSearchResult(searchNewAndBigFiles, listOf("bigbinary.dat"))
    }

    @Test
    internal fun searchWithOrClause() {
        val oldFilesOrSpecific = client.search(
            "created < 2016-03-01 OR name = subfile.txt",
            15,
            indexFile.toAbsolutePath().toString()
        ).responseBody()

        assertSearchResult(oldFilesOrSpecific, listOf("oldFile.txt", "subFile.txt"))
    }

    @Test
    internal fun searchNonExistingIndexFolder() {
        val errorResponse = client.search(
            "name = myfile.txt",
            15,
            indexFile.resolve("not-an-index").toAbsolutePath().toString()
        )

        assertEquals(Status.NOT_FOUND, errorResponse.status)
    }

    @Test
    internal fun searchEmptyIndexFolder() {
        val emptyIndexDirectory = indexFile.resolve("not-an-index")

        Files.createDirectory(emptyIndexDirectory)

        val errorResponse = client.search(
            "name = myfile.txt",
            15,
            emptyIndexDirectory.toAbsolutePath().toString()
        )

        assertEquals(Status.NOT_FOUND, errorResponse.status)
        assertEquals("Index not found or corrupted", errorResponse.rawBody())
    }

    private fun addDirectoryToIndex(
        indexFile: Path,
        directoryToIndex: Path
    ) {
        val taskId = client.indexDirectory(indexFile.toAbsolutePath().toString(), directoryToIndex.toString())
            .responseBody().taskId

        while (getTaskStatus(taskId) != TaskExecutionState.COMPLETED) {
            Thread.sleep(10L)
        }
    }

    private fun removeDirectoryFromIndex(
        directoryToIndex: Path
    ) {
        val taskId = client.deindexDirectory(indexFile.toAbsolutePath().toString(), directoryToIndex.toString())
            .responseBody().taskId

        while (getTaskStatus(taskId) != TaskExecutionState.COMPLETED) {
            Thread.sleep(10L)
        }
    }

    private fun getTaskStatus(taskId: String): TaskExecutionState {
        val response = client.taskStatus(taskId)

        return TaskExecutionState.valueOf(response.responseBody().status)
    }
}