package net.sr89.haystacker.async

import net.sr89.haystacker.async.TaskExecutionState.NOT_FOUND
import net.sr89.haystacker.server.collection.CircularQueue
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class TaskId(val id: UUID)

data class TaskStatus(val state: TaskExecutionState, val description: String)

enum class TaskExecutionState {
    NOT_FOUND, NOT_STARTED, RUNNING, COMPLETED, ERROR
}

class BackgroundTaskManager {

    private val completedTasks: CircularQueue<Pair<TaskId, BackgroundTask>> = CircularQueue(100)
    private val runningTasks = ConcurrentHashMap<TaskId, BackgroundTask>()

    fun submit(task: BackgroundTask): TaskId {
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

    fun status(taskId: TaskId): TaskStatus {
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
}