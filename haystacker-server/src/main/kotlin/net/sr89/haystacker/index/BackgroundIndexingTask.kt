package net.sr89.haystacker.index

import net.sr89.haystacker.async.BackgroundTask
import net.sr89.haystacker.async.TaskExecutionState.COMPLETED
import net.sr89.haystacker.async.TaskExecutionState.ERROR
import net.sr89.haystacker.async.TaskExecutionState.NOT_STARTED
import net.sr89.haystacker.async.TaskStatus
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference

class BackgroundIndexingTask(val indexPath: String, val directoryToIndex: Path) : BackgroundTask {

    val latestStatus = AtomicReference(TaskStatus(NOT_STARTED, "Ready to start"))

    override fun run() {
        try {
            val indexManager = IndexManager.forPath(indexPath)

            indexManager.openIndex().use {
                indexManager.indexDirectoryRecursively(it, directoryToIndex, latestStatus)
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