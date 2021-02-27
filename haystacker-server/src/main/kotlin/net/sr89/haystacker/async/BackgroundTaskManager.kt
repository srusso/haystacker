package net.sr89.haystacker.async

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class TaskId(val id: UUID)

class BackgroundTaskManager {
    private val tasks = ConcurrentHashMap<TaskId, BackgroundTask>()

    fun submit(task: BackgroundTask): TaskId {
        val id = TaskId(UUID.randomUUID())

        tasks[id] = task

        val thread = Thread { task.run() }

        thread.start()

        return id
    }

    fun isRunning(taskId: TaskId): Boolean {
        return tasks[taskId]?.running() ?: false
    }
}