package net.sr89.haystacker.filesystem

import com.sun.jna.platform.FileMonitor
import com.sun.jna.platform.FileMonitor.FILE_CREATED
import com.sun.jna.platform.FileMonitor.FILE_DELETED
import com.sun.jna.platform.FileMonitor.FILE_NAME_CHANGED_NEW
import com.sun.jna.platform.FileMonitor.FILE_NAME_CHANGED_OLD
import com.sun.jna.platform.FileMonitor.FILE_SIZE_CHANGED
import net.sr89.haystacker.index.IndexManager
import java.io.File

class IndexUpdatingListener(val indexManager: IndexManager): FileMonitor.FileListener {
    private fun isRelevantFileForThisIndex(file: File): Boolean {
        return indexManager.indexedDirectories()
            .any { indexedDirectory -> TODO("Check if file is under indexedDirectory") }
    }

    private fun fileCreated(file: File) {
        TODO("Add to index")
    }

    private fun fileDeleted(file: File) {
        TODO("Deindex")
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
            when(e.type) {
                FILE_CREATED -> fileCreated(e.file)
                FILE_DELETED -> fileDeleted(e.file)
                FILE_SIZE_CHANGED -> fileSizeChanged(e.file)
                FILE_NAME_CHANGED_OLD -> fileChangedOld(e.file)
                FILE_NAME_CHANGED_NEW -> fileChangedNew(e.file)
            }
        }
    }
}