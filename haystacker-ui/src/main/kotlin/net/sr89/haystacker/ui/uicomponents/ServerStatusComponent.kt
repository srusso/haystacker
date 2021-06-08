package net.sr89.haystacker.ui.uicomponents

import javafx.application.Platform
import javafx.scene.Node
import javafx.scene.control.Tooltip
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import net.sr89.haystacker.server.api.HaystackerRestClient
import net.sr89.haystacker.server.async.task.BackgroundTask
import net.sr89.haystacker.server.async.task.BackgroundTaskManager
import net.sr89.haystacker.server.async.task.TaskExecutionState
import net.sr89.haystacker.server.async.task.TaskStatus
import org.http4k.core.Status
import java.time.Duration

class ServerStatusComponent(
    restClient: HaystackerRestClient,
    backgroundTaskManager: BackgroundTaskManager
    ) {
    private val statusIndicator = Circle(7.0)

    init {
        setUnavailableServerStatus()

        backgroundTaskManager.submitEternally(
            object : BackgroundTask {
                override fun run() {
                    if (restClient.ping().status == Status.OK) {
                        Platform.runLater { setAvailableServerStatus() }
                    } else {
                        Platform.runLater { setUnavailableServerStatus() }
                    }
                }

                override fun interrupt() = false

                override fun currentStatus() = TaskStatus(TaskExecutionState.RUNNING, "")
            },
            Duration.ofSeconds(1)
        )
    }

    fun getStatusComponent(): Node {
        return statusIndicator
    }

    private fun setAvailableServerStatus() {
        statusIndicator.fill = Color.DARKGREEN
        val t = Tooltip("Haystacker server up and running.")
        Tooltip.install(statusIndicator, t)
    }

    private fun setUnavailableServerStatus() {
        statusIndicator.fill = Color.DARKRED
        val t = Tooltip("Haystacker server unavailable. Is it running?")
        Tooltip.install(statusIndicator, t)
    }
}