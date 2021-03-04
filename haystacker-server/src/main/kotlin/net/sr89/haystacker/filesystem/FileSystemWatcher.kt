package net.sr89.haystacker.filesystem

import com.sun.jna.platform.FileMonitor
import com.sun.jna.platform.FileMonitor.FILE_CREATED
import com.sun.jna.platform.FileMonitor.FILE_DELETED
import com.sun.jna.platform.FileMonitor.FILE_RENAMED
import com.sun.jna.platform.FileMonitor.FILE_SIZE_CHANGED
import net.sr89.haystacker.async.task.BackgroundTaskManager
import net.sr89.haystacker.index.IndexManager
import net.sr89.haystacker.index.IndexManagerProvider
import net.sr89.haystacker.server.config.SettingsManager
import java.nio.file.Path

class FileSystemWatcher(
    val indexManagerProvider: IndexManagerProvider,
    val settings: SettingsManager,
    val taskManager: BackgroundTaskManager) {
    private val activeIndexManagers: MutableSet<Long> = mutableSetOf()
    private val observedEvents = FILE_CREATED or FILE_DELETED or FILE_RENAMED or FILE_SIZE_CHANGED
    private val monitor = FileMonitor.getInstance()

    fun startWatchingIndexedDirectories() {
        for (indexPath in settings.indexes()) {
            val indexManager = indexManagerProvider.forPath(indexPath)

            for (indexedDirectory in indexManager.indexedDirectories()) {
                println("Watching $indexedDirectory")
                startWatching(indexManager, indexedDirectory)
            }
        }
    }

    fun startWatching(indexManager: IndexManager, directory: Path) {
        if (!activeIndexManagers.contains(indexManager.getUniqueManagerIdentifier())) {
            registerFSEventListenerFor(indexManager)
        }

        monitor.addWatch(directory.toFile(), observedEvents)
    }

    private fun registerFSEventListenerFor(indexManager: IndexManager) {
        val listener = IndexUpdatingListener(indexManager, taskManager)

        activeIndexManagers.add(indexManager.getUniqueManagerIdentifier())
        monitor.addFileListener(listener)
    }
}