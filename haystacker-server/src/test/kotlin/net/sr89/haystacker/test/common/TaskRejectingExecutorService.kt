package net.sr89.haystacker.test.common

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit.MILLISECONDS

class TaskRejectingExecutorService : ThreadPoolExecutor(
    10, 10, 0L, MILLISECONDS,
    LinkedBlockingQueue()
) {
    override fun execute(command: Runnable) {
        throw RejectedExecutionException()
    }
}