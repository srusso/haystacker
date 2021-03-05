package net.sr89.haystacker.async.task

import net.sr89.haystacker.async.task.TaskExecutionState.NOT_FOUND
import net.sr89.haystacker.lang.exception.InvalidTaskIdException
import net.sr89.haystacker.server.collection.CircularQueue
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class TaskId(val id: UUID) {

    companion object {
        fun fromString(taskId: String): TaskId {
            return try {
                TaskId(UUID.fromString(taskId))
            } catch (iax: IllegalArgumentException) {
                throw InvalidTaskIdException(taskId)
            }
        }
    }

}

data class TaskStatus(val state: TaskExecutionState, val description: String)

enum class TaskExecutionState {
    NOT_FOUND, NOT_STARTED, RUNNING, COMPLETED, ERROR, INTERRUPTED
}

interface BackgroundTaskManager {
    fun submit(task: BackgroundTask): TaskId
    fun status(taskId: TaskId): TaskStatus
    fun interruptAllRunningTasks()
}

class AsyncBackgroundTaskManager: BackgroundTaskManager {

    private val completedTasks: CircularQueue<Pair<TaskId, BackgroundTask>> = CircularQueue(100)
    private val runningTasks = ConcurrentHashMap<TaskId, BackgroundTask>()

    // TODO concurrency around [runningTasks], don't allow submitting new tasks if interruptAllRunningTasks() has been called
    override fun submit(task: BackgroundTask): TaskId {
        val id = TaskId(UUID.randomUUID())

        runningTasks[id] = task

        val thread = Thread {
            try {
                task.run()
            } finally {
                runningTasks.remove(id)
                completedTasks.add(Pair(id, task))
            }
        }

        thread.start()

        return id
    }

    override fun status(taskId: TaskId): TaskStatus {
        val runningTask = runningTasks[taskId]

        if (runningTask != null) {
            return runningTask.currentStatus()
        } else {
            for (completedTask in completedTasks) {
                if (completedTask.first == taskId) {
                    return completedTask.second.currentStatus()
                }
            }

            return TaskStatus(NOT_FOUND, "Not found")
        }
    }

    override fun interruptAllRunningTasks() {
        runningTasks.values.forEach(BackgroundTask::interrupt)
    }
}