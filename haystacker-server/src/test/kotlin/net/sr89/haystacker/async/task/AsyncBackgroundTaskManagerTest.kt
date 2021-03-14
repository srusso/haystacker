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

        expectTaskCompletion(taskId!!, 50)
    }

    private fun expectTaskCompletion(taskId: TaskId, timeout: Long) {
        val start = System.nanoTime()

        while (durationSince(start).toMillis() < timeout) {
            if (manager.status(taskId).state == COMPLETED) {
                return
            }
        }

        fail(AssertionError("Expected task to be completed within $timeout ms, but its state is still '${manager.status(taskId).state}'"))
    }
}