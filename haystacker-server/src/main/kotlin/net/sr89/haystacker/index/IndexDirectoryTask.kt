package net.sr89.haystacker.index

import mu.KotlinLogging
import net.sr89.haystacker.async.task.Trigger
import net.sr89.haystacker.async.task.Trigger.COMMAND
import net.sr89.haystacker.server.async.task.BackgroundTask
import net.sr89.haystacker.server.async.task.TaskExecutionState.COMPLETED
import net.sr89.haystacker.server.async.task.TaskExecutionState.ERROR
import net.sr89.haystacker.server.async.task.TaskExecutionState.INTERRUPTED
import net.sr89.haystacker.server.async.task.TaskExecutionState.NOT_STARTED
import net.sr89.haystacker.server.async.task.TaskStatus
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference

class IndexDirectoryTask(
    val trigger: Trigger,
    val indexManager: IndexManager,
    val directoryToIndex: Path
) : BackgroundTask {
    private val logger = KotlinLogging.logger {}

    val latestStatus = AtomicReference(TaskStatus(NOT_STARTED, "Ready to start"))

    override fun run() {
        try {
            indexManager.addNewDirectory(directoryToIndex, latestStatus, trigger == COMMAND)

            if (latestStatus.get().state !in setOf(INTERRUPTED, ERROR)) {
                latestStatus.set(TaskStatus(COMPLETED, "Indexed $directoryToIndex and subdirectories"))
            }
        } catch (e: Exception) {
            latestStatus.set(TaskStatus(ERROR, e.message!!))
        }
    }

    override fun interrupt(): Boolean {
        logger.info { "Interrupting task to index $directoryToIndex"}
        latestStatus.set(TaskStatus(INTERRUPTED, "Interrupt command received"))
        return true
    }

    override fun currentStatus(): TaskStatus = latestStatus.get()
}