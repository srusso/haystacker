package net.sr89.haystacker.server

import net.sr89.haystacker.index.IndexManager
import net.sr89.haystacker.lang.parser.HslParser
import net.sr89.haystacker.lang.translate.HslToLucene
import net.sr89.haystacker.server.handlers.CreateIndexHandler
import net.sr89.haystacker.server.handlers.DirectoryDeindexHandler
import net.sr89.haystacker.server.handlers.DirectoryIndexHandler
import net.sr89.haystacker.server.handlers.SearchHandler
import org.http4k.core.HttpHandler
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer

val hslToLucene = HslToLucene(HslParser())

fun quitHandler(): HttpHandler {
    return {request -> TODO("Implement me") }
}

fun haystackerRoutes(): HttpHandler {
    return routes(
        "ping" bind GET to { Response(OK) },
        "search" bind POST to SearchHandler(),
        "index" bind POST to CreateIndexHandler(),
        "directory" bind POST to DirectoryIndexHandler(),
        "directory" bind DELETE to DirectoryDeindexHandler(),
        "quit" bind POST to quitHandler()
    )
}

/**
 * Start a web server that routes requests to an [IndexManager].
 */
fun startServer(port: Int): Http4kServer {
    val app = ServerFilters.CatchLensFailure.then(haystackerRoutes())

    return app.asServer(Jetty(port)).start()
}

fun main() {
    val jettyServer = startServer(9000)

}