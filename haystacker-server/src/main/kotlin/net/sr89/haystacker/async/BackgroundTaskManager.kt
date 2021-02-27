package net.sr89.haystacker.async

import net.sr89.haystacker.async.TaskExecutionState.COMPLETED
import net.sr89.haystacker.async.TaskExecutionState.NOT_FOUND
import net.sr89.haystacker.async.TaskExecutionState.RUNNING
import net.sr89.haystacker.server.collection.CircularQueue
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class TaskId(val id: UUID)

data class TaskStatus(val id: TaskId, val state: TaskExecutionState, val description: String)

enum class TaskExecutionState {
    NOT_FOUND, RUNNING, COMPLETED
}

class BackgroundTaskManager {

    private val completedTasks: CircularQueue<TaskId> = CircularQueue(100)
    private val runningTasks = ConcurrentHashMap<TaskId, BackgroundTask>()

    fun submit(task: BackgroundTask): TaskId {
        val id = TaskId(UUID.randomUUID())

        runningTasks[id] = task

        val thread = Thread {
            task.run()

            runningTasks.remove(id)
            completedTasks.add(id)
        }

        thread.start()

        return id
    }

    fun status(taskId: TaskId): TaskStatus {
        val runningTask = runningTasks[taskId]

        if (runningTask != null) {
            return TaskStatus(taskId, RUNNING, "Running")
        } else {
            for (id in completedTasks) {
                if (id == taskId) {
                    return TaskStatus(taskId, COMPLETED, "Completed successfully")
                }
            }

            return TaskStatus(taskId, NOT_FOUND, "Not found")
        }
    }
}