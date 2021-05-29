package net.sr89.haystacker.filesystem

import mu.KotlinLogging
import net.sr89.haystacker.index.IndexManagerProvider
import net.sr89.haystacker.server.async.task.BackgroundTask
import net.sr89.haystacker.server.async.task.BackgroundTaskManager
import net.sr89.haystacker.server.async.task.TaskExecutionState.RUNNING
import net.sr89.haystacker.server.async.task.TaskStatus
import java.nio.file.FileSystems
import java.nio.file.Path
import java.time.Duration

class VolumeManager(taskManager: BackgroundTaskManager, private val indexManagerProvider: IndexManagerProvider) {
    private val logger = KotlinLogging.logger {}
    private val volumes = mutableSetOf<Path>()

    init {
        volumes.addAll(detectMountedVolumes())

        logger.info { "Found volumes $volumes" }

        taskManager.submitEternally(object : BackgroundTask {
            override fun run() {
                updateVolumeList()
            }

            override fun interrupt(): Boolean = false

            override fun currentStatus(): TaskStatus = TaskStatus(RUNNING, "Monitoring volumes")

        }, Duration.ofSeconds(1))
    }

    fun currentlyDetectedVolumes() = volumes.toList()

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
        logger.info { "Volume mounted: $volume" }

        indexManagerProvider.getAll()
            .forEach { indexManager -> indexManager.onVolumeMounted(volume) }
    }

    private fun onVolumeUnmounted(volume: Path) {
        logger.info { "Volume unmounted: $volume" }

        indexManagerProvider.getAll()
            .forEach { indexManager -> indexManager.onVolumeUnmounted(volume) }
    }
}