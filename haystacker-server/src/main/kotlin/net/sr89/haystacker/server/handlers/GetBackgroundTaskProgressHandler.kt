package net.sr89.haystacker.server.handlers

import net.sr89.haystacker.async.BackgroundTaskManager
import net.sr89.haystacker.async.TaskId
import net.sr89.haystacker.lang.exception.InvalidTaskIdException
import net.sr89.haystacker.server.api.BackgroundTaskStatusResponse
import net.sr89.haystacker.server.api.backgroundTaskStatusResponse
import net.sr89.haystacker.server.api.taskId
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import java.util.UUID

class GetBackgroundTaskProgressHandler(private val taskManager: BackgroundTaskManager): HttpHandler {

    private fun parseTaskId(taskId: String): TaskId {
        try {
            val parsedTaskId = TaskId(UUID.fromString(taskId))
            return parsedTaskId
        } catch (iax: IllegalArgumentException) {
            throw InvalidTaskIdException(taskId)
        }
    }

    override fun invoke(request: Request): Response {
        val tid: String = taskId(request)

        val status = taskManager.status(parseTaskId(tid))
        val resp = BackgroundTaskStatusResponse(tid, status.state.name, status.description)
        return Response(Status.OK)
                .with(backgroundTaskStatusResponse of resp)

    }
}
