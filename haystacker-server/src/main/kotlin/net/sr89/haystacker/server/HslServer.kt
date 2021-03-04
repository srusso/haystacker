package net.sr89.haystacker.server

import net.sr89.haystacker.async.task.AsyncBackgroundTaskManager
import net.sr89.haystacker.async.task.BackgroundTaskManager
import net.sr89.haystacker.filesystem.FileSystemWatcher
import net.sr89.haystacker.index.IndexManager
import net.sr89.haystacker.server.config.SettingsManager
import net.sr89.haystacker.server.filter.ExceptionHandlingFilter
import net.sr89.haystacker.server.handlers.CreateIndexHandler
import net.sr89.haystacker.server.handlers.DirectoryDeindexHandler
import net.sr89.haystacker.server.handlers.DirectoryIndexHandler
import net.sr89.haystacker.server.handlers.GetBackgroundTaskProgressHandler
import net.sr89.haystacker.server.handlers.SearchHandler
import org.http4k.core.HttpHandler
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.RequestContexts
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration

class HslServer(
    private val settingsManager: SettingsManager,
    private val taskManager: BackgroundTaskManager,
    private val fileSystemWatcher: FileSystemWatcher,
) {
    lateinit var serverInstance: Http4kServer

    private val shutdownDelay = Duration.ofSeconds(5)

    private fun quitHandler(): HttpHandler {
        // TODO interrupt all running tasks
        return {
            run {
                println("Shutting down in ${shutdownDelay.toSeconds()}s")
                Thread {
                    Thread.sleep(shutdownDelay.toMillis())
                    serverInstance.stop()
                }.start()
                Response(OK)
            }
        }
    }

    /**
     * Start a web server that routes requests to an [IndexManager].
     */
    private fun startRestServer(port: Int): Http4kServer {
        val contexts = RequestContexts()

        val app = ServerFilters.InitialiseRequestContext(contexts)
            .then(ExceptionHandlingFilter())
            .then(ServerFilters.CatchLensFailure())
            .then(haystackerRoutes())

        return app.asServer(Jetty(port)).start()
    }

    fun haystackerRoutes(): HttpHandler {
        return routes(
            "ping" bind GET to { Response(OK) },
            "search" bind POST to SearchHandler(),
            "index" bind POST to CreateIndexHandler(settingsManager),
            "directory" bind POST to DirectoryIndexHandler(taskManager),
            "directory" bind DELETE to DirectoryDeindexHandler(),
            "task" bind GET to GetBackgroundTaskProgressHandler(taskManager),
            "quit" bind POST to quitHandler()
        )
    }

    fun run() {
        println("Setting up filesystem watchers for existing indexes")

        fileSystemWatcher.startWatchingIndexedDirectories()

        println("Starting REST server")

        serverInstance = startRestServer(9000)

        println("Haystacker REST server started")
    }

    companion object {
        fun server(settingsDirectory: Path): HslServer {
            val haystackerSettings = SettingsManager(settingsDirectory)
            val taskManager = AsyncBackgroundTaskManager()

            // TODO https://github.com/srusso/haystacker/issues/38 - Nicer Dependency Injection
            return HslServer(
                haystackerSettings,
                taskManager,
                FileSystemWatcher(haystackerSettings, taskManager)
            )
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val settingsDirectory = if (args.isEmpty()) {
                println("Using Haystacker executable directory (${Paths.get(".").toAbsolutePath()}) as settings directory")
                Paths.get(".")
            } else {
                println("Using settings directory ${args[0]}")
                Paths.get(args[0])
            }

            server(settingsDirectory).run()
        }
    }
}