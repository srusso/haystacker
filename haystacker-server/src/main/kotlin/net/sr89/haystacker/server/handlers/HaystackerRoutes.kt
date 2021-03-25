package net.sr89.haystacker.server.handlers

import org.http4k.core.HttpHandler
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes

class HaystackerRoutes(
    val searchHandler: SearchHandler,
    val createIndexHandler: CreateIndexHandler,
    val directoryIndexHandler: DirectoryIndexHandler,
    val deindexHandler: DirectoryDeindexHandler,
    val taskProcessHandler: GetBackgroundTaskProgressHandler,
    val interruptBackgroundTaskHandler: InterruptBackgroundTaskHandler,
    val quitHandler: QuitHandler
) {
    fun routesHandler(): HttpHandler {
        return routes(
            "ping" bind GET to { Response(Status.OK) },
            "search" bind POST to searchHandler,
            "index" bind POST to createIndexHandler,
            "directory" bind POST to directoryIndexHandler,
            "directory" bind DELETE to deindexHandler,
            "task" bind GET to taskProcessHandler,
            "task/interrupt" bind POST to interruptBackgroundTaskHandler,
            "quit" bind POST to quitHandler
        )
    }
}