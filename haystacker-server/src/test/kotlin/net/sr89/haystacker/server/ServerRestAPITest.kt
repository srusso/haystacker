package net.sr89.haystacker.server

import net.sr89.haystacker.filesystem.FileSystemWatcher
import net.sr89.haystacker.server.api.directory
import net.sr89.haystacker.server.api.hslQuery
import net.sr89.haystacker.server.api.indexPath
import net.sr89.haystacker.server.api.maxResults
import net.sr89.haystacker.server.config.SettingsManager
import net.sr89.haystacker.test.common.SingleThreadTaskManager
import net.sr89.haystacker.test.common.assertSearchResult
import net.sr89.haystacker.test.common.createServerTestFiles
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneOffset

internal class HslServerKtTest {
    val oldInstant: Instant = LocalDate.of(2015, Month.APRIL, 1).atStartOfDay().toInstant(ZoneOffset.UTC)

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

        val indexRequest = Request(Method.POST, "/directory")
            .with(
                directory of directoryToIndex.toString(),
                indexPath of indexFile.toAbsolutePath().toString()
            )

        removeSubdirectoryFromIndexRequest = Request(Method.DELETE, "/directory")
            .with(
                directory of subDirectory.toString(),
                indexPath of indexFile.toAbsolutePath().toString()
            )

        val settingsManager = SettingsManager(settingsDirectory)
        val taskManager = SingleThreadTaskManager()
        val fsWatcher = FileSystemWatcher(settingsManager, taskManager)
        val server = HslServer(settingsManager, taskManager, fsWatcher)

        routes = server.haystackerRoutes()

        routes(createRequest)
        routes(indexRequest)
    }

    @AfterEach
    internal fun tearDown() {
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
}