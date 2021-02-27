package net.sr89.haystacker.index

import net.sr89.haystacker.async.BackgroundTask
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

class BackgroundIndexingTask(val indexPath: String, val directoryToIndex: Path): BackgroundTask {
    val done = AtomicBoolean(false)

    override fun run() {
        val indexManager = IndexManager.forPath(indexPath)

        indexManager.openIndex().use {
            indexManager.indexDirectoryRecursively(it, directoryToIndex)
        }

        done.set(true)
    }

    override fun interrupt() {
        TODO("Not yet implemented")
    }

    override fun running() = !done.get()
}