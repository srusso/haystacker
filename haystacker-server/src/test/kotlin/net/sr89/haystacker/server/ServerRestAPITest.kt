package net.sr89.haystacker.server

import com.fasterxml.jackson.databind.ObjectMapper
import net.sr89.haystacker.async.task.BackgroundTaskManager
import net.sr89.haystacker.async.task.TaskExecutionState
import net.sr89.haystacker.server.api.directory
import net.sr89.haystacker.server.api.hslQuery
import net.sr89.haystacker.server.api.indexPath
import net.sr89.haystacker.server.api.maxResults
import net.sr89.haystacker.server.config.ServerConfig
import net.sr89.haystacker.server.handlers.HaystackerRoutes
import net.sr89.haystacker.test.common.NoOpServer
import net.sr89.haystacker.test.common.SingleThreadTaskManager
import net.sr89.haystacker.test.common.TaskCreatedResponseType
import net.sr89.haystacker.test.common.TaskStatusResponseType
import net.sr89.haystacker.test.common.assertSearchResult
import net.sr89.haystacker.test.common.createServerTestFiles
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.server.Http4kServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.multiton
import org.kodein.di.singleton
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneOffset

internal class HaystackerApplicationKtTest {
    private val oldInstant: Instant = LocalDate.of(2015, Month.APRIL, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
    private val objectMapper = ObjectMapper()

    lateinit var routes: HttpHandler
    lateinit var directoryToIndex: Path
    lateinit var settingsDirectory: Path
    lateinit var subDirectory: Path
    lateinit var indexFile: Path
    lateinit var removeSubdirectoryFromIndexRequest: Request

    @BeforeEach
    internal fun setUp() {
        directoryToIndex = Files.createTempDirectory("files")
        settingsDirectory = Files.createTempDirectory("settings")
        subDirectory = directoryToIndex.resolve("subdirectory")
        indexFile = Files.createTempDirectory("index")

        createServerTestFiles(oldInstant, directoryToIndex, subDirectory)

        val createRequest = Request(Method.POST, "/index")
            .with(
                indexPath of indexFile.toAbsolutePath().toString()
            )

        removeSubdirectoryFromIndexRequest = Request(Method.DELETE, "/directory")
            .with(
                directory of subDirectory.toString(),
                indexPath of indexFile.toAbsolutePath().toString()
            )

        val testOverrides = DI.Module("DITestOverrides") {
            bind<BackgroundTaskManager>(overrides = true) with singleton { SingleThreadTaskManager() }
            bind<Http4kServer>(overrides = true) with multiton { conf: ServerConfig -> NoOpServer() }
        }

        val testDI = DI {
            import(handlersModule)
            import(managerModule)

            import(testOverrides, allowOverride = true)
        }

        val config = ServerConfig(0, Paths.get(""))
        val myRoutes: HaystackerRoutes by testDI.instance(arg = config)

        routes = myRoutes.routesHandler()

        routes(createRequest)
        addDirectoryToIndex(indexFile, directoryToIndex)
    }

    @AfterEach
    internal fun tearDown() {
        println("Shutting down server: ${routes(Request(Method.POST, "/quit"))}")

        directoryToIndex.toFile().deleteRecursively()
        settingsDirectory.toFile().deleteRecursively()
        indexFile.toFile().deleteRecursively()
    }

    @Test
    internal fun searchByFilename() {
        val searchFileByNameInDirectory = Request(Method.POST, "/search")
            .with(
                hslQuery of "name = oldfile.txt",
                maxResults of 15,
                indexPath of indexFile.toAbsolutePath().toString()
            )

        val searchFileByNameInSubDirectory = Request(Method.POST, "/search")
            .with(
                hslQuery of "name = \"subfile.txt\"",
                maxResults of 15,
                indexPath of indexFile.toAbsolutePath().toString()
            )

        assertSearchResult(routes(searchFileByNameInDirectory), listOf("oldfile.txt"))
        assertSearchResult(routes(searchFileByNameInSubDirectory), listOf("subfile.txt"))

        routes(removeSubdirectoryFromIndexRequest)

        assertSearchResult(routes(searchFileByNameInDirectory), listOf("oldfile.txt"))
        assertSearchResult(routes(searchFileByNameInSubDirectory), emptyList())
    }

    @Test
    internal fun searchByFileSize() {
        val searchSmallFiles = Request(Method.POST, "/search")
            .with(
                hslQuery of "size < 500",
                maxResults of 15,
                indexPath of indexFile.toAbsolutePath().toString()
            )

        val searchBigFiles = Request(Method.POST, "/search")
            .with(
                hslQuery of "size > 500",
                maxResults of 15,
                indexPath of indexFile.toAbsolutePath().toString()
            )

        assertSearchResult(routes(searchBigFiles), listOf("bigbinary.dat"))
        assertSearchResult(routes(searchSmallFiles), listOf("oldfile.txt", "binary.dat", "subfile.txt"))
    }

    @Test
    internal fun searchByLastModifiedTime() {
        val searchOldFiles = Request(Method.POST, "/search")
            .with(
                hslQuery of "last_modified < 2016-03-01",
                maxResults of 15,
                indexPath of indexFile.toAbsolutePath().toString()
            )

        val searchNewFiles = Request(Method.POST, "/search")
            .with(
                hslQuery of "last_modified > 2016-03-01",
                maxResults of 15,
                indexPath of indexFile.toAbsolutePath().toString()
            )

        assertSearchResult(routes(searchOldFiles), listOf("oldfile.txt"))
        assertSearchResult(routes(searchNewFiles), listOf("bigbinary.dat", "binary.dat", "subfile.txt"))
    }

    @Test
    internal fun searchByCreatedTime() {
        val searchOldFiles = Request(Method.POST, "/search")
            .with(
                hslQuery of "created < 2016-03-01",
                maxResults of 15,
                indexPath of indexFile.toAbsolutePath().toString()
            )

        val searchNewFiles = Request(Method.POST, "/search")
            .with(
                hslQuery of "created > 2016-03-01",
                maxResults of 15,
                indexPath of indexFile.toAbsolutePath().toString()
            )

        assertSearchResult(routes(searchOldFiles), listOf("oldfile.txt"))
        assertSearchResult(routes(searchNewFiles), listOf("bigbinary.dat", "binary.dat", "subfile.txt"))
    }

    @Test
    internal fun searchWithAndClause() {
        val searchNewAndBigFiles = Request(Method.POST, "/search")
            .with(
                hslQuery of "created > 2016-03-01 AND size > 500",
                maxResults of 15,
                indexPath of indexFile.toAbsolutePath().toString()
            )

        assertSearchResult(routes(searchNewAndBigFiles), listOf("bigbinary.dat"))
    }

    @Test
    internal fun searchWithOrClause() {
        val oldFilesOrSpecific = Request(Method.POST, "/search")
            .with(
                hslQuery of "created < 2016-03-01 OR name = subfile.txt",
                maxResults of 15,
                indexPath of indexFile.toAbsolutePath().toString()
            )

        assertSearchResult(routes(oldFilesOrSpecific), listOf("oldfile.txt", "subfile.txt"))
    }

    private fun addDirectoryToIndex(
        indexFile: Path,
        directoryToIndex: Path
    ) {
        val taskResponse = routes(Request(Method.POST, "/directory")
            .with(
                directory of directoryToIndex.toString(),
                indexPath of indexFile.toAbsolutePath().toString()
            ))

        val taskId = objectMapper.readValue(taskResponse.bodyString(), TaskCreatedResponseType())!!

        while (getTaskStatus(taskId.taskId) != TaskExecutionState.COMPLETED) {
            Thread.sleep(10L)
        }
    }

    private fun getTaskStatus(taskId: String): TaskExecutionState {
        val response = routes(Request(Method.GET, "/task")
            .with(
                net.sr89.haystacker.server.api.taskId of taskId
            ))

        return TaskExecutionState.valueOf(objectMapper.readValue(response.bodyString(), TaskStatusResponseType())!!.status)
    }
}