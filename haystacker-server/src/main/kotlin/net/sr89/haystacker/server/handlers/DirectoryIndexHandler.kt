package net.sr89.haystacker.server.handlers

import mu.KotlinLogging
import net.sr89.haystacker.async.task.Trigger.COMMAND
import net.sr89.haystacker.index.IndexDirectoryTask
import net.sr89.haystacker.index.IndexManagerProvider
import net.sr89.haystacker.server.api.TaskIdResponse
import net.sr89.haystacker.server.api.directoryQuery
import net.sr89.haystacker.server.api.indexPathQuery
import net.sr89.haystacker.server.api.stringBody
import net.sr89.haystacker.server.api.taskIdResponse
import net.sr89.haystacker.server.async.task.BackgroundTaskManager
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import java.nio.file.Paths

class DirectoryIndexHandler(
    private val indexManagerProvider: IndexManagerProvider,
    private val taskManager: BackgroundTaskManager
): HttpHandler {

    private val logger = KotlinLogging.logger {}

    override fun invoke(request: Request): Response {
        val indexPath: String = indexPathQuery(request)
        val directoryToIndex = Paths.get(directoryQuery(request))

        logger.info { "Received request to add directory $directoryToIndex to index $indexPath" }

        return if (!directoryToIndex.toFile().exists()) {
            Response(Status.NOT_FOUND).with(stringBody of "Directory $directoryToIndex not found")
        } else if (!Paths.get(indexPath).toFile().exists()) {
            Response(Status.NOT_FOUND).with(stringBody of "Index at $indexPath not found")
        } else {
            val taskId = taskManager.submit(IndexDirectoryTask(COMMAND, indexManagerProvider.forPath(indexPath), directoryToIndex))

            if (taskId != null) {
                Response(Status.OK).with(taskIdResponse of TaskIdResponse(taskId.id.toString()))
            } else {
                Response(Status.SERVICE_UNAVAILABLE).with(stringBody of "Task was not started")
            }
        }
    }
}