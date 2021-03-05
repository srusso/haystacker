package net.sr89.haystacker.filesystem

import com.sun.jna.platform.FileMonitor
import com.sun.jna.platform.FileMonitor.FILE_CREATED
import com.sun.jna.platform.FileMonitor.FILE_DELETED
import com.sun.jna.platform.FileMonitor.FILE_NAME_CHANGED_NEW
import com.sun.jna.platform.FileMonitor.FILE_NAME_CHANGED_OLD
import com.sun.jna.platform.FileMonitor.FILE_SIZE_CHANGED
import net.sr89.haystacker.async.task.BackgroundTaskManager
import net.sr89.haystacker.index.BackgroundIndexingTask
import net.sr89.haystacker.index.IndexManager
import net.sr89.haystacker.index.Trigger.FS_UPDATE
import java.io.File
import java.nio.file.Path

class IndexUpdatingListener(
    val indexManager: IndexManager,
    val taskManager: BackgroundTaskManager) : FileMonitor.FileListener {
    private fun Path.isParentOf(otherPath: Path): Boolean {
        return otherPath.toAbsolutePath().toString().startsWith(toAbsolutePath().toString())
    }

    private fun isRelevantFileForThisIndex(file: File): Boolean {
        val filePath: Path = file.toPath()
        return indexManager.indexedDirectories()
            .any { indexedDirectory -> indexedDirectory.isParentOf(filePath) }
            .and(indexManager.excludedDirectories()
                .none { excludedDirectory -> excludedDirectory.isParentOf(filePath) })
    }

    private fun fileCreated(file: File) {
        println("File $file was created")
        taskManager.submit(BackgroundIndexingTask(FS_UPDATE, indexManager, file.toPath()))
    }

    private fun fileDeleted(file: File) {
        println("File $file was deleted")
        indexManager.removeDirectoryFromIndex(file.toPath(), false)
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
        if (isRelevantFileForThisIndex(e.file)) {
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