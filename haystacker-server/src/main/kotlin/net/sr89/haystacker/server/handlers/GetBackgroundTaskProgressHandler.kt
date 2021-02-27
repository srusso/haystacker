package net.sr89.haystacker.server.handlers

import net.sr89.haystacker.async.BackgroundTaskManager
import net.sr89.haystacker.async.TaskId
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
    override fun invoke(request: Request): Response {
        val tid: String = taskId(request)

        return Response(Status.OK).with(backgroundTaskStatusResponse of BackgroundTaskStatusResponse(tid, taskManager.isRunning(TaskId(UUID.fromString(tid)))))
    }
}