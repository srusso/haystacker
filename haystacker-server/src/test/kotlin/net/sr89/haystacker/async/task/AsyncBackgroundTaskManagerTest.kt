package net.sr89.haystacker.async.task

import net.sr89.haystacker.async.task.TaskExecutionState.COMPLETED
import net.sr89.haystacker.async.task.TaskExecutionState.INTERRUPTED
import net.sr89.haystacker.async.task.TaskExecutionState.NOT_STARTED
import net.sr89.haystacker.async.task.TaskExecutionState.RUNNING
import net.sr89.haystacker.test.common.durationSince
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertNull

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

    @Test
    internal fun submitTaskThatCompletesSuccessfully() {
        val taskId = manager.submit(MockBackgroundTask(30))

        expectTaskCompleted(taskId!!, 500)
    }

    @Test
    internal fun submitManyTasks() {
        val taskDuration = 10L

        val taskIds = (1..50).map {
            manager.submit(MockBackgroundTask(taskDuration))!!
        }

        expectAllTaskCompletedWithin(taskIds, 5000)
    }

    @Test
    internal fun shutdownWhileTaskIsRunning() {
        val taskId = manager.submit(MockBackgroundTask(Duration.ofSeconds(30).toMillis()))

        manager.shutdownAndWaitForTasksToComplete()

        expectTaskInterrupted(taskId!!, 500)

        // new tasks are not started after shutdownAndWaitForTasksToComplete() is called
        assertNull(manager.submit(MockBackgroundTask(1000L)))
    }

    private fun expectAllTaskCompletedWithin(taskIds: List<TaskId>, timeout: Long) {
        val start = System.nanoTime()

        var remainingTasks = taskIds.toList()

        while (durationSince(start).toMillis() < timeout) {
            remainingTasks = remainingTasks.filter{
                !taskHasState(it, COMPLETED)
            }
            if (remainingTasks.isEmpty()) {
                return
            }
            Thread.sleep(5L)
        }

        fail(AssertionError("Expected all ${taskIds.size} tasks to be completed within $timeout ms, but ${remainingTasks.size} tasks are still running"))
    }

    private fun expectTaskInterrupted(taskId: TaskId, timeout: Long) {
        expectTaskStateWithinTimeout(taskId, INTERRUPTED ,timeout)
    }

    private fun expectTaskCompleted(taskId: TaskId, timeout: Long) {
        expectTaskStateWithinTimeout(taskId, COMPLETED ,timeout)
    }

    private fun expectTaskStateWithinTimeout(taskId: TaskId, state: TaskExecutionState, timeout: Long) {
        val start = System.nanoTime()

        while (durationSince(start).toMillis() < timeout) {
            if (taskHasState(taskId, state)) {
                return
            }
            Thread.sleep(5L)
        }

        fail(AssertionError("Expected task to be completed within $timeout ms, but its state is still '${manager.status(taskId).state}'"))
    }

    private fun taskHasState(taskId: TaskId, state: TaskExecutionState) = manager.status(taskId).state == state
}