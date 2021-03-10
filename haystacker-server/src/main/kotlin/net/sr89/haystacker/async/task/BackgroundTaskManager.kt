package net.sr89.haystacker.async.task

import net.sr89.haystacker.async.task.TaskExecutionState.NOT_FOUND
import net.sr89.haystacker.lang.exception.InvalidTaskIdException
import net.sr89.haystacker.server.collection.CircularQueue
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit.SECONDS

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
    /**
     * Submits and immediately run the task.
     *
     * @return A [TaskId] that uniquely identifies the new task, or null if the task couldn't be started
     */
    fun submit(task: BackgroundTask): TaskId?
    fun status(taskId: TaskId): TaskStatus
    fun shutdownAndWaitForTasksToComplete()
}

class AsyncBackgroundTaskManager : BackgroundTaskManager {

    private val completedTasks: CircularQueue<Pair<TaskId, BackgroundTask>> = CircularQueue(100)
    private val runningTasks = ConcurrentHashMap<TaskId, BackgroundTask>()
    private val executor = Executors.newFixedThreadPool(15)

    override fun submit(task: BackgroundTask): TaskId? {
        if (executor.isShutdown) {
            println("Not starting task of type ${task::class} because the server is being shut down")
            return null
        }

        val id = TaskId(UUID.randomUUID())

        try {
            CompletableFuture.runAsync(
                {
                    runningTasks[id] = task
                    task.run()
                }, executor)
                .whenComplete { _, _ ->
                    runningTasks.remove(id)
                    completedTasks.add(Pair(id, task))
                }
        } catch (e: RejectedExecutionException) {
            e.printStackTrace()
            return null
        }

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

    override fun shutdownAndWaitForTasksToComplete() {
        executor.shutdown()

        println("Waiting up to 30 seconds for all currently running tasks to complete")

        executor.awaitTermination(30, SECONDS)
    }
}