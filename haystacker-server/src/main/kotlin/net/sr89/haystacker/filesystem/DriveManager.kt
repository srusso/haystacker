package net.sr89.haystacker.filesystem

import net.sr89.haystacker.async.task.BackgroundTask
import net.sr89.haystacker.async.task.BackgroundTaskManager
import net.sr89.haystacker.async.task.TaskExecutionState.RUNNING
import net.sr89.haystacker.async.task.TaskStatus
import java.nio.file.FileSystems
import java.nio.file.Path
import java.time.Duration

class DriveManager(taskManager: BackgroundTaskManager) {
    private val drives = mutableSetOf<Path>()

    init {
        drives.addAll(
            FileSystems.getDefault().rootDirectories
        )

        println("Found drives $drives")

        taskManager.submitEternally(object :  BackgroundTask {
            override fun run() {
                updateDriveList()
            }

            override fun interrupt(): Boolean = false

            override fun currentStatus(): TaskStatus = TaskStatus(RUNNING, "Monitoring drives")

        }, Duration.ofSeconds(5))
    }

    fun updateDriveList() {
        val newDrives = FileSystems.getDefault().rootDirectories

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

//        if (driveAddedOrRemoved) {
            println("New set of drives: $drives")
//        }
    }

    private fun onDriveMounted(drive: Path) {
        println("New drive mounted: $drive")
        TODO("Start file system watcher if needed (any index watches it or subdirectories)")
    }

    private fun onDriveUnmounted(drive: Path) {
        println("Drive unmounted: $drive")
        TODO("Stop any file system watchers on this drive or subdirectories")
    }
}