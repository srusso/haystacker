package net.sr89.haystacker.filesystem

import com.sun.jna.platform.FileMonitor
import com.sun.jna.platform.FileMonitor.FILE_CREATED
import com.sun.jna.platform.FileMonitor.FILE_DELETED
import com.sun.jna.platform.FileMonitor.FILE_RENAMED
import com.sun.jna.platform.FileMonitor.FILE_SIZE_CHANGED
import net.sr89.haystacker.index.IndexManager
import net.sr89.haystacker.server.config.HaystackerSettings

class FileSystemWatcher(val haystackerSettings: HaystackerSettings) {
    private val observedEvents = FILE_CREATED or FILE_DELETED or FILE_RENAMED or FILE_SIZE_CHANGED

    fun startWatchingIndexedDirectories() {
        val monitor = FileMonitor.getInstance()

        for (indexPath in haystackerSettings.indexes()) {
            val indexManager = IndexManager.forPath(indexPath)
            val listener = IndexUpdatingListener(indexManager)

            monitor.addFileListener(listener)

            for (indexedDirectory in indexManager.indexedDirectories()) {
                monitor.addWatch(indexedDirectory.toFile(), observedEvents)
            }
        }
    }
}