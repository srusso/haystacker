package net.sr89.haystacker.test.common

import net.sr89.haystacker.async.task.BackgroundTask
import net.sr89.haystacker.async.task.BackgroundTaskManager
import net.sr89.haystacker.async.task.TaskExecutionState.COMPLETED
import net.sr89.haystacker.async.task.TaskId
import net.sr89.haystacker.async.task.TaskStatus
import java.util.UUID

class SingleThreadTaskManager: BackgroundTaskManager {
    override fun submit(task: BackgroundTask): TaskId {
        task.run()
        return TaskId.fromString(UUID.randomUUID().toString())
    }

    override fun status(taskId: TaskId): TaskStatus = TaskStatus(COMPLETED, "Done")

    override fun shutdownAndWaitForTasksToComplete() {

    }
}