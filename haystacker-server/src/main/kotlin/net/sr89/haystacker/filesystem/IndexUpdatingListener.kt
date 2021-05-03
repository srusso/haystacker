package net.sr89.haystacker.filesystem

import com.sun.jna.platform.FileMonitor
import com.sun.jna.platform.FileMonitor.FILE_CREATED
import com.sun.jna.platform.FileMonitor.FILE_DELETED
import com.sun.jna.platform.FileMonitor.FILE_NAME_CHANGED_NEW
import com.sun.jna.platform.FileMonitor.FILE_NAME_CHANGED_OLD
import com.sun.jna.platform.FileMonitor.FILE_SIZE_CHANGED
import mu.KotlinLogging
import net.sr89.haystacker.async.task.BackgroundTaskManager
import net.sr89.haystacker.async.task.Trigger.FS_UPDATE
import net.sr89.haystacker.index.DeindexDirectoryTask
import net.sr89.haystacker.index.IndexDirectoryTask
import net.sr89.haystacker.index.IndexManager
import java.io.File

class IndexUpdatingListener(
    val indexManager: IndexManager,
    val taskManager: BackgroundTaskManager) : FileMonitor.FileListener {
    private val logger = KotlinLogging.logger {}

    private fun fileCreated(file: File) {
        logger.info { "File $file was created"}
        taskManager.submit(IndexDirectoryTask(FS_UPDATE, indexManager, file.toPath()))
    }

    private fun fileDeleted(file: File) {
        logger.info { "File $file was deleted"}
        taskManager.submit(DeindexDirectoryTask(FS_UPDATE, indexManager, file.toPath()))
    }

    private fun fileChangedOld(file: File) {
        fileDeleted(file)
    }

    private fun fileChangedNew(file: File) {
        fileCreated(file)
    }

    private fun fileSizeChanged(file: File) {
        fileCreated(file)
    }

    override fun fileChanged(e: FileMonitor.FileEvent) {
        if (indexManager.fileIsRelevantForIndex(e.file)) {
            when (e.type) {
                FILE_CREATED -> fileCreated(e.file)
                FILE_DELETED -> fileDeleted(e.file)
                FILE_SIZE_CHANGED -> fileSizeChanged(e.file)
                FILE_NAME_CHANGED_OLD -> fileChangedOld(e.file)
                FILE_NAME_CHANGED_NEW -> fileChangedNew(e.file)
            }
        }
    }
}