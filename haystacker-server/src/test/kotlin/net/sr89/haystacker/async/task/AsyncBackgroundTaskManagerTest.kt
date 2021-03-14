package net.sr89.haystacker.async.task

import net.sr89.haystacker.async.task.TaskExecutionState.COMPLETED
import net.sr89.haystacker.async.task.TaskExecutionState.INTERRUPTED
import net.sr89.haystacker.async.task.TaskExecutionState.NOT_STARTED
import net.sr89.haystacker.async.task.TaskExecutionState.RUNNING
import net.sr89.haystacker.test.common.durationSince
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

private class MockBackgroundTask(val durationMillis: Long): BackgroundTask {
    private val interrupted = AtomicBoolean(false)
    private val status = AtomicReference(TaskStatus(NOT_STARTED, ""))

    override fun run() {
        status.set(TaskStatus(RUNNING, ""))

        val loopDuration = 10L
        val loops = durationMillis/loopDuration

        for (i in 1..loops) {
            Thread.sleep(loopDuration)
            if (interrupted.get()) {
                status.set(TaskStatus(INTERRUPTED, ""))
                return
            }
        }

        status.set(TaskStatus(COMPLETED, ""))
    }

    override fun interrupt() {
        interrupted.set(true)
    }

    override fun currentStatus(): TaskStatus = status.get()

}

internal class AsyncBackgroundTaskManagerTest {

    lateinit var manager: AsyncBackgroundTaskManager

    @BeforeEach
    fun setUp() {
        manager = AsyncBackgroundTaskManager()
    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    internal fun submitTaskThatCompletesSuccessfully() {
        val taskId = manager.submit(MockBackgroundTask(30))

        expectTaskCompletion(taskId!!, 500)
    }

    @Test
    internal fun submitManyTasks() {
        val taskDuration = 10L

        val taskIds = (1..50).map {
            manager.submit(MockBackgroundTask(taskDuration))!!
        }

        expectAllTaskCompletedWithin(taskIds, 5000)
    }

    private fun expectAllTaskCompletedWithin(taskIds: List<TaskId>, timeout: Long) {
        val start = System.nanoTime()

        var remainingTasks = taskIds.toList()

        while (durationSince(start).toMillis() < timeout) {
            remainingTasks = remainingTasks.filter{
                !isTaskCompleted(it)
            }
            if (remainingTasks.isEmpty()) {
                return
            }
        }

        fail(AssertionError("Expected all ${taskIds.size} tasks to be completed within $timeout ms, but ${remainingTasks.size} tasks are still running"))
    }

    private fun expectTaskCompletion(taskId: TaskId, timeout: Long) {
        val start = System.nanoTime()

        while (durationSince(start).toMillis() < timeout) {
            if (isTaskCompleted(taskId)) {
                return
            }
        }

        fail(AssertionError("Expected task to be completed within $timeout ms, but its state is still '${manager.status(taskId).state}'"))
    }

    private fun isTaskCompleted(taskId: TaskId) = manager.status(taskId).state == COMPLETED
}