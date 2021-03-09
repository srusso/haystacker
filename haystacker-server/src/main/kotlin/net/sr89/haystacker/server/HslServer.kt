package net.sr89.haystacker.server

import net.sr89.haystacker.async.task.BackgroundTaskManager
import net.sr89.haystacker.index.IndexManager
import net.sr89.haystacker.index.IndexManagerProvider
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
import org.kodein.di.instance
import org.kodein.di.newInstance
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration

class HslServer(
    private val indexManagerProvider: IndexManagerProvider,
    private val settingsManager: SettingsManager,
    private val taskManager: BackgroundTaskManager,
    private val shutdownDelay: Duration
) {
    lateinit var serverInstance: Http4kServer

    private fun quitHandler(): HttpHandler {
        return {
            run {

                println("Stopping all filesystem watchers")
                indexManagerProvider.getAll().forEach(IndexManager::stopWatchingFileSystemChanges)
                println("Interrupting all running background tasks")
                taskManager.interruptAllRunningTasks()

                println("Shutting down in ${shutdownDelay.toMillis()}ms")
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
            "search" bind POST to SearchHandler(indexManagerProvider),
            "index" bind POST to CreateIndexHandler(indexManagerProvider, settingsManager),
            "directory" bind POST to DirectoryIndexHandler(indexManagerProvider, taskManager),
            "directory" bind DELETE to DirectoryDeindexHandler(indexManagerProvider),
            "task" bind GET to GetBackgroundTaskProgressHandler(taskManager),
            "quit" bind POST to quitHandler()
        )
    }

    fun run() {
        println("Setting up filesystem watchers for existing indexes")

        settingsManager.indexes()
            .map(indexManagerProvider::forPath)
            .forEach(IndexManager::startWatchingFileSystemChanges)

        println("Starting REST server")

        serverInstance = startRestServer(9000)

        println("Haystacker REST server started")
    }

    companion object {
        fun server(settingsDirectory: Path, shutdownDelay: Duration = Duration.ofSeconds(5)): HslServer {
            val hslServer by serverModule().newInstance {
                HslServer(
                    instance(),
                    instance(arg = settingsDirectory),
                    instance(),
                    shutdownDelay
                )
            }

            return hslServer
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