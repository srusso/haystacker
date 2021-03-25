package net.sr89.haystacker.server

import com.fasterxml.jackson.databind.ObjectMapper
import net.sr89.haystacker.async.task.TaskExecutionState
import net.sr89.haystacker.async.task.TaskExecutionState.COMPLETED
import net.sr89.haystacker.server.api.directory
import net.sr89.haystacker.server.api.hslQuery
import net.sr89.haystacker.server.api.indexPath
import net.sr89.haystacker.server.api.maxResults
import net.sr89.haystacker.server.config.ServerConfig
import net.sr89.haystacker.test.common.TaskCreatedResponseType
import net.sr89.haystacker.test.common.TaskStatusResponseType
import net.sr89.haystacker.test.common.assertSearchResult
import net.sr89.haystacker.test.common.createServerTestFiles
import net.sr89.haystacker.test.common.tryAssertingRepeatedly
import org.http4k.client.ApacheClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.with
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.singleton
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneOffset

internal class ServerFSMonitoringTest {
    private fun <R> HaystackerApplication.runServer(testCase: HaystackerApplication.() -> R) {
        try {
            this.run()
            testCase()
        } finally {
            quitServer()
        }
    }

    private lateinit var httpClient: HttpHandler
    private val objectMapper = ObjectMapper()
    private val shutdownDelay = Duration.ofMillis(10L)

    val oldInstant: Instant = LocalDate.of(2015, Month.APRIL, 1).atStartOfDay().toInstant(ZoneOffset.UTC)

    private val baseUrl = "http://localhost:9000"

    lateinit var directoryToIndex: Path

    lateinit var settingsDirectory: Path
    lateinit var subDirectory: Path
    lateinit var indexFile: Path

    @BeforeEach
    internal fun setUp() {
        directoryToIndex = Files.createTempDirectory("files")
        settingsDirectory = Files.createTempDirectory("settings")
        subDirectory = directoryToIndex.resolve("subdirectory")
        indexFile = Files.createTempDirectory("index")

        createServerTestFiles(oldInstant, directoryToIndex, subDirectory)
    }

    @AfterEach
    internal fun tearDown() {
        directoryToIndex.toFile().deleteRecursively()
        settingsDirectory.toFile().deleteRecursively()
        indexFile.toFile().deleteRecursively()
    }

    /**
     * Start server
     * Create index
     * Add directories to index
     * Remove some of the subdirectories
     *
     * Make one search to see that everything is OK
     *
     * Add, rename, delete.. etc. Files in watched directories.
     *
     * Check that search results reflect those changes.
     *
     * Restart the server. Check that all still works by making more changes to the files and more searches
     *    - basically this makes sure that settings were loaded, and that correct FS watchers were set up
     *
     * Clean up test files.
     */
    @Test
    internal fun testFileSystemWatching() {
        newServer().runServer {
            createIndex(indexFile)
            addDirectoryToIndex(indexFile, directoryToIndex)

            assertSearchResult(searchIndex(indexFile, "name = oldfile.txt"), listOf("oldFile.txt"))
            assertSearchResult(searchIndex(indexFile, "name = newfile.txt"), listOf())
            assertSearchResult(searchIndex(indexFile, "name = subfile.txt"), listOf("subFile.txt"))

            Files.newOutputStream(directoryToIndex.resolve("newFile.txt")).use {
                it.write("The file system watcher should pick up that this file was created!".toByteArray())
            }

            removeDirectoryFromIndex(indexFile, subDirectory)

            // let's give it some time to pick up changes from the file system - try repeatedly for 1 second until success or timeout
            tryAssertingRepeatedly(Duration.ofSeconds(1)) {
                assertSearchResult(searchIndex(indexFile, "name = newfile.txt"), listOf("newFile.txt"), "Expected new file to be added to the index by the file system watcher")
                assertSearchResult(searchIndex(indexFile, "name = subfile.txt"), listOf(), "Subdirectory was removed from the index")
            }
        }

        println()

        newServer().runServer {
            assertSearchResult(searchIndex(indexFile, "name = newfile.txt"), listOf("newFile.txt"))

            Files.newOutputStream(directoryToIndex.resolve("fileCreatedAfterRestart.txt")).use {
                it.write("The file system watcher should pick up that this file was created!".toByteArray())
            }

            Files.newOutputStream(subDirectory.resolve("ignoredbecauseinremoveddirectory.txt")).use {
                it.write("The file system watcher should pick up that this file was created!".toByteArray())
            }

            directoryToIndex.resolve("newFile.txt").toFile().delete()

            // let's give it some time to pick up changes from the file system - try repeatedly for 1 second until success or timeout
            // TODO give list of actions/asserts to try, and only retry those who fail
            tryAssertingRepeatedly(Duration.ofSeconds(1)) {
                assertSearchResult(searchIndex(indexFile, "name = newfile.txt"), listOf(), "The file was removed, so it shouldn't be returned by the search anymore")
                assertSearchResult(
                    searchIndex(indexFile, "name = ignoredbecauseinremoveddirectory.txt"),
                    listOf(),
                    "Subdirectory $subDirectory was excluded from the index, so we are not expecting changes there to be picked up by the file system watcher and updated in the index again"
                )
                assertSearchResult(
                    searchIndex(indexFile, "name = filecreatedafterrestart.txt"),
                    listOf("fileCreatedAfterRestart.txt"),
                    "Expected file in $directoryToIndex to be indexed by the file system watcher even after restarting the server"
                )
            }
        }
    }

    private fun newServer(): HaystackerApplication {
        httpClient = ApacheClient()

        val config = ServerConfig(9000, settingsDirectory)

        val testOverrides = DI.Module("DITestOverrides") {
            bind<Duration>(overrides = true, tag = "shutdownDelay") with singleton { shutdownDelay }
        }

        val testDI = DI {
            import(utilModule)
            import(handlersModule)
            import(managerModule(config))

            import(testOverrides, allowOverride = true)
        }

        return HaystackerApplication.application(testDI)
    }

    private fun createIndex(indexFile: Path) {
        httpClient(Request(Method.POST, "$baseUrl/index")
            .with(
                indexPath of indexFile.toAbsolutePath().toString()
            ))
    }

    private fun quitServer() {
        httpClient(Request(Method.POST, "$baseUrl/quit"))

        Thread.sleep(shutdownDelay.toMillis() * 3)
    }

    private fun addDirectoryToIndex(
        indexFile: Path,
        directoryToIndex: Path
    ) {
        val taskResponse = httpClient(Request(Method.POST, "$baseUrl/directory")
            .with(
                directory of directoryToIndex.toString(),
                indexPath of indexFile.toAbsolutePath().toString()
            ))

        val taskId = objectMapper.readValue(taskResponse.bodyString(), TaskCreatedResponseType())!!

        while (getTaskStatus(taskId.taskId) != COMPLETED) {
            Thread.sleep(10L)
        }
    }

    private fun removeDirectoryFromIndex(
        indexFile: Path,
        directoryToIndex: Path
    ) {
        httpClient(Request(Method.DELETE, "$baseUrl/directory")
            .with(
                directory of directoryToIndex.toString(),
                indexPath of indexFile.toAbsolutePath().toString()
            ))
    }

    private fun getTaskStatus(taskId: String): TaskExecutionState {
        val response = httpClient(Request(Method.GET, "$baseUrl/task")
            .with(
                net.sr89.haystacker.server.api.taskId of taskId
            ))

        return TaskExecutionState.valueOf(objectMapper.readValue(response.bodyString(), TaskStatusResponseType())!!.status)
    }

    private fun searchIndex(indexFile: Path, searchQuery: String): Response {
        return httpClient(Request(Method.POST, "$baseUrl/search")
            .with(
                hslQuery of searchQuery,
                maxResults of 15,
                indexPath of indexFile.toAbsolutePath().toString()
            ))
    }
}