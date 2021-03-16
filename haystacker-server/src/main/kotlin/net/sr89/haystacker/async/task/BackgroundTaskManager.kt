package net.sr89.haystacker.async.task

import net.sr89.haystacker.async.task.TaskExecutionState.NOT_FOUND
import net.sr89.haystacker.lang.exception.InvalidTaskIdException
import net.sr89.haystacker.server.collection.FifoConcurrentMap
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
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

class AsyncBackgroundTaskManager(private val executor: ExecutorService) : BackgroundTaskManager {

    private val finishedTasks: FifoConcurrentMap<TaskId, BackgroundTask> = FifoConcurrentMap(100)
    private val runningTasks = ConcurrentHashMap<TaskId, BackgroundTask>()

    override fun submit(task: BackgroundTask): TaskId? {
        if (executor.isShutdown) {
            println("Not starting task of type ${task::class} because the server is being shut down")
            return null
        }

        val id = TaskId(UUID.randomUUID())

        runningTasks[id] = task

        try {
            CompletableFuture.runAsync(task::run, executor)
                .whenComplete { _, _ ->
                    runningTasks.remove(id)
                    finishedTasks.put(id, task)
                }
        } catch (e: RejectedExecutionException) {
            println("The task was rejected by the executor service: ${e.message}")
            runningTasks.remove(id)
            return null
        }

        return id
    }

    override fun status(taskId: TaskId): TaskStatus {
        val runningTask = runningTasks[taskId]

        return if (runningTask != null) {
            runningTask.currentStatus()
        } else {
            val finishedTask = finishedTasks.get(taskId)

            finishedTask?.currentStatus() ?: TaskStatus(NOT_FOUND, "Not found")
        }
    }

    override fun shutdownAndWaitForTasksToComplete() {
        executor.shutdown()

        runningTasks.forEach { (_, task) -> task.interrupt() }

        println("Waiting up to 30 seconds for all currently running tasks to complete")

        executor.awaitTermination(30, SECONDS)
    }

    internal fun runningTasks(): List<TaskId> {
        return runningTasks.keys.toList()
    }
}