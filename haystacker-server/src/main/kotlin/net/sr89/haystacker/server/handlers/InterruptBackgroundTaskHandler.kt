package net.sr89.haystacker.server.handlers

import net.sr89.haystacker.server.api.interruptBackgroundTaskResponse
import net.sr89.haystacker.server.api.taskIdQuery
import net.sr89.haystacker.server.async.task.BackgroundTaskManager
import net.sr89.haystacker.server.async.task.TaskId
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with

class InterruptBackgroundTaskHandler(private val taskManager: BackgroundTaskManager): HttpHandler {
    override fun invoke(request: Request): Response {
        val taskId = TaskId.fromString(taskIdQuery(request))

        return Response(Status.OK)
                .with(interruptBackgroundTaskResponse of taskManager.sendInterrupt(taskId))
    }
}
