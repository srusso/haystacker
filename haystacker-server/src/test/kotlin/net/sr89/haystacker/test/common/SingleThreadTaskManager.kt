package net.sr89.haystacker.test.common

import net.sr89.haystacker.server.api.TaskInterruptResponse
import net.sr89.haystacker.server.async.task.BackgroundTask
import net.sr89.haystacker.server.async.task.BackgroundTaskManager
import net.sr89.haystacker.server.async.task.TaskExecutionState.COMPLETED
import net.sr89.haystacker.server.async.task.TaskId
import net.sr89.haystacker.server.async.task.TaskStatus
import java.time.Duration
import java.util.UUID

class SingleThreadTaskManager: BackgroundTaskManager {
    override fun submit(task: BackgroundTask): TaskId {
        task.run()
        return TaskId.fromString(UUID.randomUUID().toString())
    }

    override fun submitEternally(task: BackgroundTask, interval: Duration): TaskId? {
        TODO("Not yet implemented")
    }

    override fun status(taskId: TaskId): TaskStatus = TaskStatus(COMPLETED, "Done")

    override fun sendInterrupt(taskId: TaskId): TaskInterruptResponse {
        TODO("Not yet implemented")
    }

    override fun shutdownAndWaitForTasksToComplete() {

    }
}