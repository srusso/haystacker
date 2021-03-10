package net.sr89.haystacker.server.handlers

import net.sr89.haystacker.async.task.BackgroundTaskManager
import net.sr89.haystacker.index.IndexManager
import net.sr89.haystacker.index.IndexManagerProvider
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.server.Http4kServer
import java.time.Duration

class QuitHandler(private val indexManagerProvider: IndexManagerProvider,
                  private val taskManager: BackgroundTaskManager,
                  private val shutdownDelay: Duration) : HttpHandler {
    lateinit var serverInstance: Http4kServer

    override fun invoke(request: Request): Response {
        println("Stopping all filesystem watchers")
        indexManagerProvider.getAll().forEach(IndexManager::stopWatchingFileSystemChanges)
        println("Interrupting all running background tasks")
        taskManager.shutdownAndWaitForTasksToComplete()

        println("Shutting down in ${shutdownDelay.toMillis()}ms")

        // cannot use the taskManager itself here because we are shutting it down
        Thread {
            Thread.sleep(shutdownDelay.toMillis())
            serverInstance.stop()
        }.start()
        return Response(Status.OK)
    }
}