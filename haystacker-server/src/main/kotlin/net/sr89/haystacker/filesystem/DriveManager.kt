package net.sr89.haystacker.filesystem

import net.sr89.haystacker.async.task.BackgroundTask
import net.sr89.haystacker.async.task.BackgroundTaskManager
import net.sr89.haystacker.async.task.TaskExecutionState.RUNNING
import net.sr89.haystacker.async.task.TaskStatus
import net.sr89.haystacker.index.IndexManagerProvider
import java.nio.file.FileSystems
import java.nio.file.Path
import java.time.Duration

class DriveManager(taskManager: BackgroundTaskManager, private val indexManagerProvider: IndexManagerProvider) {
    private val drives = mutableSetOf<Path>()

    init {
        drives.addAll(detectMountedDrives())

        println("Found drives $drives")

        taskManager.submitEternally(object : BackgroundTask {
            override fun run() {
                updateDriveList()
            }

            override fun interrupt(): Boolean = false

            override fun currentStatus(): TaskStatus = TaskStatus(RUNNING, "Monitoring drives")

        }, Duration.ofSeconds(1))
    }

    fun updateDriveList() {
        val newDrives = detectMountedDrives()

        var driveAddedOrRemoved = false

        newDrives.forEach { newDrive ->
            if (drives.add(newDrive)) {
                driveAddedOrRemoved = true
                onDriveMounted(newDrive)
            }
        }

        val drivesToRemove = drives.filterNot { drive -> newDrives.contains(drive) }

        drivesToRemove.forEach { removedDrive ->
            driveAddedOrRemoved = true
            onDriveUnmounted(removedDrive)
        }

        println("Drives to remove: $drivesToRemove")
        drives.removeAll(drivesToRemove)

        if (driveAddedOrRemoved) {
            println("Mounted drives: $drives")
        }
    }

    private fun detectMountedDrives() = FileSystems.getDefault().rootDirectories

    private fun onDriveMounted(drive: Path) {
        println("New drive mounted: $drive")

        indexManagerProvider.getAll()
            .forEach{indexManager -> indexManager.onDriveMounted(drive)}
    }

    private fun onDriveUnmounted(drive: Path) {
        println("Drive unmounted: $drive")

        indexManagerProvider.getAll()
            .forEach{indexManager -> indexManager.onDriveUnmounted(drive)}
    }
}