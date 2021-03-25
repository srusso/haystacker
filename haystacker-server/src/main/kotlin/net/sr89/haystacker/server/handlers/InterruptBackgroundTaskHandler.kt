package net.sr89.haystacker.server.handlers

import net.sr89.haystacker.async.task.BackgroundTaskManager
import net.sr89.haystacker.async.task.TaskId
import net.sr89.haystacker.server.api.interruptBackgroundTaskResponse
import net.sr89.haystacker.server.api.taskId
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with

class InterruptBackgroundTaskHandler(private val taskManager: BackgroundTaskManager): HttpHandler {
    override fun invoke(request: Request): Response {
        val taskId = TaskId.fromString(taskId(request))

        return Response(Status.OK)
                .with(interruptBackgroundTaskResponse of taskManager.sendInterrupt(taskId))
    }
}
