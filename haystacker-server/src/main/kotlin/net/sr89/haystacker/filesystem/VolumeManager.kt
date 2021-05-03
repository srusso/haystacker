package net.sr89.haystacker.filesystem

import net.sr89.haystacker.async.task.BackgroundTask
import net.sr89.haystacker.async.task.BackgroundTaskManager
import net.sr89.haystacker.async.task.TaskExecutionState.RUNNING
import net.sr89.haystacker.async.task.TaskStatus
import net.sr89.haystacker.index.IndexManagerProvider
import java.nio.file.FileSystems
import java.nio.file.Path
import java.time.Duration

class VolumeManager(taskManager: BackgroundTaskManager, private val indexManagerProvider: IndexManagerProvider) {
    private val volumes = mutableSetOf<Path>()

    init {
        volumes.addAll(detectMountedVolumes())

        println("Found volumes $volumes")

        taskManager.submitEternally(object : BackgroundTask {
            override fun run() {
                updateVolumeList()
            }

            override fun interrupt(): Boolean = false

            override fun currentStatus(): TaskStatus = TaskStatus(RUNNING, "Monitoring volumes")

        }, Duration.ofSeconds(1))
    }

    fun updateVolumeList() {
        val newVolumes = detectMountedVolumes()

        newVolumes.forEach { newVolume ->
            if (volumes.add(newVolume)) {
                onVolumeMounted(newVolume)
            }
        }

        val volumesToRemove = volumes.filterNot(newVolumes::contains)

        volumesToRemove.forEach(this::onVolumeUnmounted)

        volumes.removeAll(volumesToRemove)
    }

    private fun detectMountedVolumes() = FileSystems.getDefault().rootDirectories

    private fun onVolumeMounted(volume: Path) {
        println("Volume mounted: $volume")

        indexManagerProvider.getAll()
            .forEach{indexManager -> indexManager.onVolumeMounted(volume)}
    }

    private fun onVolumeUnmounted(volume: Path) {
        println("Volume unmounted: $volume")

        indexManagerProvider.getAll()
            .forEach{indexManager -> indexManager.onVolumeUnmounted(volume)}
    }
}