package net.sr89.haystacker.filesystem

import com.sun.jna.platform.FileMonitor
import com.sun.jna.platform.FileMonitor.FILE_CREATED
import com.sun.jna.platform.FileMonitor.FILE_DELETED
import com.sun.jna.platform.FileMonitor.FILE_RENAMED
import com.sun.jna.platform.FileMonitor.FILE_SIZE_CHANGED
import net.sr89.haystacker.async.task.BackgroundTaskManager
import net.sr89.haystacker.index.IndexManager
import net.sr89.haystacker.server.config.SettingsManager

class FileSystemWatcher(val settings: SettingsManager, val taskManager: BackgroundTaskManager) {
    private val observedEvents = FILE_CREATED or FILE_DELETED or FILE_RENAMED or FILE_SIZE_CHANGED

    fun startWatchingIndexedDirectories() {
        val monitor = FileMonitor.getInstance()

        for (indexPath in settings.indexes()) {
            val indexManager = IndexManager.forPath(indexPath)
            val listener = IndexUpdatingListener(indexManager, taskManager)

            monitor.addFileListener(listener)

            for (indexedDirectory in indexManager.indexedDirectories()) {
                monitor.addWatch(indexedDirectory.toFile(), observedEvents)
            }
        }
    }
}