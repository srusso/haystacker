package net.sr89.haystacker.server

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import net.sr89.haystacker.server.handlers.SearchResponse
import net.sr89.haystacker.server.handlers.directory
import net.sr89.haystacker.server.handlers.hslQuery
import net.sr89.haystacker.server.handlers.indexPath
import net.sr89.haystacker.server.handlers.maxResults
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.with
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals

internal class HslServerKtTest {
    val routes = haystackerRoutes()

    @Test
    internal fun operateOnIndexViaRESTRoutes() {
        val directoryToIndex = Files.createTempDirectory("files")
        val subDirectory = directoryToIndex.resolve("subdirectory")
        val indexFile = Files.createTempDirectory("index")

        addTestFilesTo(directoryToIndex, subDirectory)

        val createRequest = Request(Method.POST, "/index")
            .with(
                indexPath of indexFile.toAbsolutePath().toString()
            )

        val indexRequest = Request(Method.POST, "/directory")
            .with(
                directory of directoryToIndex.toString(),
                indexPath of indexFile.toAbsolutePath().toString()
            )

        val removeSubdirectoryFromIndexRequest = Request(Method.DELETE, "/directory")
            .with(
                directory of subDirectory.toString(),
                indexPath of indexFile.toAbsolutePath().toString()
            )

        val searchFileInDirectory = Request(Method.POST, "/search")
            .with(
                hslQuery of "name = abba.txt",
                maxResults of 15,
                indexPath of indexFile.toAbsolutePath().toString()
            )

        val searchFileInSubDirectory = Request(Method.POST, "/search")
            .with(
                hslQuery of "name = \"subfile.txt\"",
                maxResults of 15,
                indexPath of indexFile.toAbsolutePath().toString()
            )

        routes(createRequest)
        routes(indexRequest)
        assertSearchResult(routes(searchFileInDirectory), 1)
        assertSearchResult(routes(searchFileInSubDirectory), 1)

        routes(removeSubdirectoryFromIndexRequest)

        assertSearchResult(routes(searchFileInDirectory), 1)
        assertSearchResult(routes(searchFileInSubDirectory), 0)
    }

    private fun assertSearchResult(response: Response, expectedResultCount: Long) {
        class SearchResponseType: TypeReference<SearchResponse>()

        val searchResponse = ObjectMapper().readValue(response.bodyString(), SearchResponseType())

        assertEquals(searchResponse.totalResults, expectedResultCount)
    }

    private fun addTestFilesTo(directoryToIndex: Path, subDirectory: Path) {
        Files.newOutputStream(directoryToIndex.resolve("abba.txt")).use {
            it.write("Some example file contents".toByteArray())
        }

        Files.newOutputStream(directoryToIndex.resolve("binary.dat")).use {
            it.write(ByteArray(10) { i -> i.toByte() })
        }

        Files.createDirectory(subDirectory)

        Files.newOutputStream(subDirectory.resolve("subfile.txt")).use {
            it.write("Some example file contents (subdirectory file)".toByteArray())
        }
    }
}