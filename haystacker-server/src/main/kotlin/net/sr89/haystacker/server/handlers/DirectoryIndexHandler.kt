package net.sr89.haystacker.server.handlers

import net.sr89.haystacker.async.task.BackgroundTaskManager
import net.sr89.haystacker.index.BackgroundIndexingTask
import net.sr89.haystacker.index.Trigger.COMMAND
import net.sr89.haystacker.server.api.DirectoryIndexResponse
import net.sr89.haystacker.server.api.directory
import net.sr89.haystacker.server.api.directoryIndexResponse
import net.sr89.haystacker.server.api.indexPath
import net.sr89.haystacker.server.api.stringBody
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import java.nio.file.Paths

class DirectoryIndexHandler(private val taskManager: BackgroundTaskManager): HttpHandler {
    override fun invoke(request: Request): Response {
        val indexPath: String = indexPath(request)
        val directoryToIndex = Paths.get(directory(request))

        println("Received request to add directory $directoryToIndex to index $indexPath")

        return if (!directoryToIndex.toFile().exists()) {
            Response(Status.NOT_FOUND).with(stringBody of "Directory $directoryToIndex not found")
        } else if (!Paths.get(indexPath).toFile().exists()) {
            Response(Status.NOT_FOUND).with(stringBody of "Index at $indexPath not found")
        } else {
            val taskId = taskManager.submit(BackgroundIndexingTask(COMMAND, indexPath, directoryToIndex))

            Response(Status.OK).with(directoryIndexResponse of DirectoryIndexResponse(taskId.id.toString()))
        }
    }
}