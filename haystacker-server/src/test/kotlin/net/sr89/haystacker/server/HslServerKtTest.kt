package net.sr89.haystacker.server

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import net.sr89.haystacker.server.api.SearchResponse
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
import kotlin.test.assertEquals

internal class HslServerKtTest {
    val routes = haystackerRoutes()

    var directoryToIndex: Path? = null
    var subDirectory: Path? = null
    var indexFile: Path? = null

    var removeSubdirectoryFromIndexRequest: Request? = null
    var searchFileByNameInDirectory: Request? = null
    var searchFileByNameInSubDirectory: Request? = null
    var searchSmallFiles: Request? = null
    var searchBigFiles: Request? = null

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

        searchFileByNameInDirectory = Request(Method.POST, "/search")
            .with(
                hslQuery of "name = abba.txt",
                maxResults of 15,
                indexPath of indexFile!!.toAbsolutePath().toString()
            )

        searchFileByNameInSubDirectory = Request(Method.POST, "/search")
            .with(
                hslQuery of "name = \"subfile.txt\"",
                maxResults of 15,
                indexPath of indexFile!!.toAbsolutePath().toString()
            )

        searchSmallFiles = Request(Method.POST, "/search")
            .with(
                hslQuery of "size < 500kb",
                maxResults of 15,
                indexPath of indexFile!!.toAbsolutePath().toString()
            )

        searchBigFiles = Request(Method.POST, "/search")
            .with(
                hslQuery of "size > 500kb",
                maxResults of 15,
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
        assertSearchResult(routes(searchFileByNameInDirectory!!), 1)
        assertSearchResult(routes(searchFileByNameInSubDirectory!!), 1)

        routes(removeSubdirectoryFromIndexRequest!!)

        assertSearchResult(routes(searchFileByNameInDirectory!!), 1)
        assertSearchResult(routes(searchFileByNameInSubDirectory!!), 0)
    }

    @Test
    internal fun searchByFileSize() {
        assertSearchResult(routes(searchBigFiles!!), 1)
        assertSearchResult(routes(searchSmallFiles!!), 3)
    }

    private fun assertSearchResult(response: Response, expectedResultCount: Long) {
        class SearchResponseType: TypeReference<SearchResponse>()

        val searchResponse = ObjectMapper().readValue(response.bodyString(), SearchResponseType())

        assertEquals(expectedResultCount, searchResponse.totalResults)
    }

    private fun addTestFilesTo(directoryToIndex: Path, subDirectory: Path) {
        Files.newOutputStream(directoryToIndex.resolve("abba.txt")).use {
            it.write("Some example file contents".toByteArray())
        }

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