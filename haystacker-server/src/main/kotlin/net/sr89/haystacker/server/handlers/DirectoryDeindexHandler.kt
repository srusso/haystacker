package net.sr89.haystacker.server.handlers

import net.sr89.haystacker.async.task.BackgroundTaskManager
import net.sr89.haystacker.async.task.Trigger.COMMAND
import net.sr89.haystacker.index.DeindexDirectoryTask
import net.sr89.haystacker.index.IndexManagerProvider
import net.sr89.haystacker.server.api.TaskIdResponse
import net.sr89.haystacker.server.api.directoryQuery
import net.sr89.haystacker.server.api.indexPathQuery
import net.sr89.haystacker.server.api.stringBody
import net.sr89.haystacker.server.api.taskIdResponse
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import java.nio.file.Paths

class DirectoryDeindexHandler(
    val taskManager: BackgroundTaskManager,
    val indexManagerProvider: IndexManagerProvider): HttpHandler {
    override fun invoke(request: Request): Response {
        val indexPath: String = indexPathQuery(request)
        val directoryToDeindex = Paths.get(directoryQuery(request))

        println("Received request to remove directory $directoryToDeindex from index $indexPath")

        return if (!directoryToDeindex.toFile().exists()) {
            Response(Status.NOT_FOUND).with(stringBody of "Directory $directoryToDeindex not found")
        } else if (!Paths.get(indexPath).toFile().exists()) {
            Response(Status.NOT_FOUND).with(stringBody of "Index at $indexPath not found")
        } else {
            val indexManager = indexManagerProvider.forPath(indexPath)

            val taskId = taskManager.submit(DeindexDirectoryTask(COMMAND, indexManager, directoryToDeindex))

            if (taskId != null) {
                Response(Status.OK).with(taskIdResponse of TaskIdResponse(taskId.id.toString()))
            } else {
                Response(Status.SERVICE_UNAVAILABLE).with(stringBody of "Task was not started")
            }
        }
    }
}