package net.sr89.haystacker.server

import net.sr89.haystacker.index.IndexManager
import net.sr89.haystacker.server.api.stringBody
import net.sr89.haystacker.server.handlers.CreateIndexHandler
import net.sr89.haystacker.server.handlers.DirectoryDeindexHandler
import net.sr89.haystacker.server.handlers.DirectoryIndexHandler
import net.sr89.haystacker.server.handlers.SearchHandler
import org.http4k.core.HttpHandler
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer
import java.time.Duration

private var serverInstance: Http4kServer? = null

private val shutdownDelay = Duration.ofSeconds(5)

fun quitHandler(): HttpHandler {
    return {
        if (serverInstance != null) {
            println("Shutting down in ${shutdownDelay.toSeconds()}s")
            Thread {
                Thread.sleep(shutdownDelay.toMillis())
                serverInstance!!.stop()
            }.start()
            Response(OK)
        } else {
            Response(Status.INTERNAL_SERVER_ERROR)
                .with(stringBody of "Server not running.. but still received a request to shut down?")
        }
    }
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
    serverInstance = startServer(9000)

    println("Haystacker server started")
}