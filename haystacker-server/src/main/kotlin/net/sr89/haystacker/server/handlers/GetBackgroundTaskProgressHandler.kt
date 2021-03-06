package net.sr89.haystacker.server.handlers

import net.sr89.haystacker.server.api.BackgroundTaskStatusResponse
import net.sr89.haystacker.server.api.backgroundTaskStatusResponse
import net.sr89.haystacker.server.api.taskIdQuery
import net.sr89.haystacker.server.async.task.BackgroundTaskManager
import net.sr89.haystacker.server.async.task.TaskId
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with

class GetBackgroundTaskProgressHandler(private val taskManager: BackgroundTaskManager): HttpHandler {

    override fun invoke(request: Request): Response {
        val tid: String = taskIdQuery(request)

        val status = taskManager.status(TaskId.fromString(tid))
        val resp = BackgroundTaskStatusResponse(tid, status.state.name, status.description)
        return Response(Status.OK)
                .with(backgroundTaskStatusResponse of resp)
    }
}
