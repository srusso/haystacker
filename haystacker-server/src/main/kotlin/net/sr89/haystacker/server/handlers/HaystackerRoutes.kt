package net.sr89.haystacker.server.handlers

import org.http4k.core.HttpHandler
import org.http4k.core.Method
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
    val quitHandler: QuitHandler
) {
    fun routesHandler(): HttpHandler {
        return routes(
            "ping" bind Method.GET to { Response(Status.OK) },
            "search" bind Method.POST to searchHandler,
            "index" bind Method.POST to createIndexHandler,
            "directory" bind Method.POST to directoryIndexHandler,
            "directory" bind Method.DELETE to deindexHandler,
            "task" bind Method.GET to taskProcessHandler,
            "quit" bind Method.POST to quitHandler
        )
    }
}