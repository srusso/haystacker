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
import java.io.File
import java.nio.file.Path

class FileSystemWatcher(
    val indexManagerProvider: IndexManagerProvider,
    val settings: SettingsManager,
    val taskManager: BackgroundTaskManager) {
    private val activeIndexManagers: MutableSet<Long> = mutableSetOf()
    private val watchedDirectories: MutableSet<File> = mutableSetOf()
    private val listeners: MutableSet<IndexUpdatingListener> = mutableSetOf()
    private val observedEvents = FILE_CREATED or FILE_DELETED or FILE_RENAMED or FILE_SIZE_CHANGED
    private val monitor = FileMonitor.getInstance()

    fun stopWatchingAll() {
        watchedDirectories.forEach(monitor::removeWatch)
        listeners.forEach(monitor::removeFileListener)
    }

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
        if (!activeIndexManagers.contains(indexManager.getUniqueIdentifier())) {
            registerFSEventListenerFor(indexManager)
        }

        val directoryFile = directory.toFile()

        watchedDirectories.add(directoryFile)
        monitor.addWatch(directoryFile, observedEvents)
    }

    private fun registerFSEventListenerFor(indexManager: IndexManager) {
        val listener = IndexUpdatingListener(indexManager, taskManager)

        activeIndexManagers.add(indexManager.getUniqueIdentifier())
        monitor.addFileListener(listener)
        listeners.add(listener)
    }
}