package net.sr89.haystacker.index

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

class DeindexDirectoryTask(
    val trigger: Trigger,
    val indexManager: IndexManager,
    val directoryToIndex: Path
) : BackgroundTask {

    val latestStatus = AtomicReference(TaskStatus(NOT_STARTED, "Ready to start"))

    override fun run() {
        try {
            indexManager.removeDirectory(directoryToIndex, trigger == COMMAND)

            if (latestStatus.get().state !in setOf(INTERRUPTED, ERROR)) {
                latestStatus.set(TaskStatus(COMPLETED, "Removed $directoryToIndex and subdirectories from index"))
            }
        } catch (e: Exception) {
            latestStatus.set(TaskStatus(ERROR, e.message!!))
        }
    }

    // currently, this task is not interruptible
    override fun interrupt() = false

    override fun currentStatus(): TaskStatus = latestStatus.get()
}