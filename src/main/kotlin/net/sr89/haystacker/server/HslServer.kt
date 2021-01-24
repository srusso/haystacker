package net.sr89.haystacker.server

import net.sr89.haystacker.index.IndexManager
import net.sr89.haystacker.lang.parser.HslParser
import net.sr89.haystacker.lang.translate.HslToLucene
import net.sr89.haystacker.server.handlers.CreateIndexHandler
import net.sr89.haystacker.server.handlers.DirectoryDeindexHandler
import net.sr89.haystacker.server.handlers.DirectoryIndexHandler
import net.sr89.haystacker.server.handlers.SearchHandler
import net.sr89.haystacker.server.handlers.directory
import net.sr89.haystacker.server.handlers.hslQuery
import net.sr89.haystacker.server.handlers.indexPath
import net.sr89.haystacker.server.handlers.maxResults
import org.http4k.client.ApacheClient
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer
import java.nio.file.Files

val hslToLucene = HslToLucene(HslParser())

/**
 * Start a web server that routes requests to an [IndexManager].
 */
private fun startServer(port: Int): Http4kServer {
    val routes = routes(
        "ping" bind GET to { Response(OK) },
        "search" bind POST to SearchHandler(),
        "index" bind POST to CreateIndexHandler(),
        "directory" bind POST to DirectoryIndexHandler(),
        "directory" bind DELETE to DirectoryDeindexHandler()
    )

    val app = ServerFilters.CatchLensFailure.then(routes)

    return app.asServer(Jetty(port)).start()
}

fun main() {
    val jettyServer = startServer(9000)

    val indexFile = Files.createTempDirectory("index").toFile()

    val createRequest = Request(POST, "http://localhost:9000/index")
        .with(
            indexPath of indexFile.absolutePath
        )

    val indexRequest = Request(POST, "http://localhost:9000/directory")
        .with(
            directory of "D:\\random",
            indexPath of indexFile.absolutePath
        )

    val removeFromIndexRequest = Request(DELETE, "http://localhost:9000/directory")
        .with(
            directory of "D:\\random",
            indexPath of indexFile.absolutePath
        )

    val searchRequest = Request(POST, "http://localhost:9000/search")
        .with(
            hslQuery of "name = abba.txt",
            maxResults of 15,
            indexPath of indexFile.absolutePath
        )

    val client = ApacheClient()

    client(createRequest)
    client(indexRequest)
    println(client(searchRequest))

    Thread.sleep(1000L)

    client(removeFromIndexRequest)

    Thread.sleep(1000L)

    println(client(searchRequest))

    jettyServer.stop()
}