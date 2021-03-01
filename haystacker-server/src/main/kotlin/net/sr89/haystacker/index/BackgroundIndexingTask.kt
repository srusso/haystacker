package net.sr89.haystacker.index

import net.sr89.haystacker.async.task.BackgroundTask
import net.sr89.haystacker.async.task.TaskExecutionState.COMPLETED
import net.sr89.haystacker.async.task.TaskExecutionState.ERROR
import net.sr89.haystacker.async.task.TaskExecutionState.NOT_STARTED
import net.sr89.haystacker.async.task.TaskStatus
import net.sr89.haystacker.index.Trigger.COMMAND
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference

enum class Trigger { FS_UPDATE, COMMAND }

class BackgroundIndexingTask(val trigger: Trigger, val indexPath: String, val directoryToIndex: Path) : BackgroundTask {

    val latestStatus = AtomicReference(TaskStatus(NOT_STARTED, "Ready to start"))

    override fun run() {
        try {
            val indexManager = IndexManager.forPath(indexPath)

            indexManager.openIndex().use {
                indexManager.addNewDirectoryToIndex(it, directoryToIndex, latestStatus, trigger == COMMAND)
            }

            latestStatus.set(TaskStatus(COMPLETED, "Indexed $directoryToIndex and subdirectories"))
        } catch (e: Exception) {
            latestStatus.set(TaskStatus(ERROR, e.message!!))
        }
    }

    override fun interrupt() {
        TODO("Not yet implemented")
    }

    override fun currentStatus(): TaskStatus = latestStatus.get()
}