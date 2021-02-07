package net.sr89.haystacker.server

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import net.sr89.haystacker.server.api.SearchResponse
import net.sr89.haystacker.server.api.SearchResult
import net.sr89.haystacker.server.api.directory
import net.sr89.haystacker.server.api.hslQuery
import net.sr89.haystacker.server.api.indexPath
import net.sr89.haystacker.server.api.maxResults
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.with
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.util.unit.DataSize
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneOffset
import kotlin.test.assertTrue

fun Path.setTimes(lastModified: Instant, created: Instant): Path {
    Files.getFileAttributeView(this, BasicFileAttributeView::class.java)
        .setTimes(FileTime.from(lastModified), null, FileTime.from(created))
    return this
}

class SearchResponseType : TypeReference<SearchResponse>()

internal class HslServerKtTest {
    val routes = haystackerRoutes()

    val oldInstant: Instant = LocalDate.of(2015, Month.APRIL, 1).atStartOfDay().toInstant(ZoneOffset.UTC)

    var directoryToIndex: Path? = null
    var subDirectory: Path? = null
    var indexFile: Path? = null

    var removeSubdirectoryFromIndexRequest: Request? = null

    @BeforeEach
    internal fun setUp() {
        directoryToIndex = Files.createTempDirectory("files")
        subDirectory = directoryToIndex!!.resolve("subdirectory")
        indexFile = Files.createTempDirectory("index")

        addTestFilesTo(directoryToIndex!!, subDirectory!!)

        val createRequest = Request(Method.POST, "/index")
            .with(
                indexPath of indexFile!!.toAbsolutePath().toString()
            )

        val indexRequest = Request(Method.POST, "/directory")
            .with(
                directory of directoryToIndex.toString(),
                indexPath of indexFile!!.toAbsolutePath().toString()
            )

        removeSubdirectoryFromIndexRequest = Request(Method.DELETE, "/directory")
            .with(
                directory of subDirectory.toString(),
                indexPath of indexFile!!.toAbsolutePath().toString()
            )

        routes(createRequest)
        routes(indexRequest)
    }

    @AfterEach
    internal fun tearDown() {
        directoryToIndex!!.toFile().deleteRecursively()
        indexFile!!.toFile().deleteRecursively()
    }

    @Test
    internal fun searchByFilename() {
        val searchFileByNameInDirectory = Request(Method.POST, "/search")
            .with(
                hslQuery of "name = oldfile.txt",
                maxResults of 15,
                indexPath of indexFile!!.toAbsolutePath().toString()
            )

        val searchFileByNameInSubDirectory = Request(Method.POST, "/search")
            .with(
                hslQuery of "name = \"subfile.txt\"",
                maxResults of 15,
                indexPath of indexFile!!.toAbsolutePath().toString()
            )

        assertSearchResult(routes(searchFileByNameInDirectory), listOf("oldfile.txt"))
        assertSearchResult(routes(searchFileByNameInSubDirectory), listOf("subfile.txt"))

        routes(removeSubdirectoryFromIndexRequest!!)

        assertSearchResult(routes(searchFileByNameInDirectory), listOf("oldfile.txt"))
        assertSearchResult(routes(searchFileByNameInSubDirectory), emptyList())
    }

    @Test
    internal fun searchByFileSize() {
        val searchSmallFiles = Request(Method.POST, "/search")
            .with(
                hslQuery of "size < 500kb",
                maxResults of 15,
                indexPath of indexFile!!.toAbsolutePath().toString()
            )

        val searchBigFiles = Request(Method.POST, "/search")
            .with(
                hslQuery of "size > 500kb",
                maxResults of 15,
                indexPath of indexFile!!.toAbsolutePath().toString()
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
                indexPath of indexFile!!.toAbsolutePath().toString()
            )

        val searchNewFiles = Request(Method.POST, "/search")
            .with(
                hslQuery of "last_modified > 2016-03-01",
                maxResults of 15,
                indexPath of indexFile!!.toAbsolutePath().toString()
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
                indexPath of indexFile!!.toAbsolutePath().toString()
            )

        val searchNewFiles = Request(Method.POST, "/search")
            .with(
                hslQuery of "created > 2016-03-01",
                maxResults of 15,
                indexPath of indexFile!!.toAbsolutePath().toString()
            )

        assertSearchResult(routes(searchOldFiles), listOf("oldfile.txt"))
        assertSearchResult(routes(searchNewFiles), listOf("bigbinary.dat", "binary.dat", "subfile.txt"))
    }

    @Test
    internal fun searchWithAndClause() {
        val searchNewAndBigFiles = Request(Method.POST, "/search")
            .with(
                hslQuery of "created > 2016-03-01 AND size > 500kb",
                maxResults of 15,
                indexPath of indexFile!!.toAbsolutePath().toString()
            )

        assertSearchResult(routes(searchNewAndBigFiles), listOf("bigbinary.dat"))
    }

    @Test
    internal fun searchWithOrClause() {
        val oldFilesOrSpecific = Request(Method.POST, "/search")
            .with(
                hslQuery of "created < 2016-03-01 OR name = subfile.txt",
                maxResults of 15,
                indexPath of indexFile!!.toAbsolutePath().toString()
            )

        assertSearchResult(routes(oldFilesOrSpecific), listOf("oldfile.txt", "subfile.txt"))
    }

    private fun assertSearchResult(response: Response, expectedFilenames: List<String>) {
        val searchResponse = ObjectMapper().readValue(response.bodyString(), SearchResponseType())
        val foundFilenames: List<String> = searchResponse.results
            .map(SearchResult::path)
            .map { path -> Paths.get(path).fileName.toString() }

        expectedFilenames.forEach { path ->
            assertTrue(path in foundFilenames, "Expected path $path not found among results: $foundFilenames")
        }
    }

    private fun addTestFilesTo(directoryToIndex: Path, subDirectory: Path) {
        val oldFile = directoryToIndex.resolve("oldfile.txt")

        Files.newOutputStream(oldFile).use {
            it.write("Some example file contents".toByteArray())
        }
        oldFile.setTimes(oldInstant, oldInstant)

        Files.newOutputStream(directoryToIndex.resolve("binary.dat")).use {
            it.write(ByteArray(10) { i -> i.toByte() })
        }

        Files.newOutputStream(directoryToIndex.resolve("bigbinary.dat")).use {
            it.write(ByteArray(DataSize.ofMegabytes(1).toBytes().toInt()) { i -> i.toByte() })
        }


        Files.createDirectory(subDirectory)

        Files.newOutputStream(subDirectory.resolve("subfile.txt")).use {
            it.write("Some example file contents (subdirectory file)".toByteArray())
        }
    }
}