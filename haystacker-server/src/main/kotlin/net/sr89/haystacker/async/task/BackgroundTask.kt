package net.sr89.haystacker.async.task

interface BackgroundTask {
    fun run()

    /**
     * Gently tell the task to stop running as soon as possible.
     *
     * @return true if the task expects to stop soon, false otherwise (for example, it's a non-interruptible task)
     */
    fun interrupt(): Boolean

    fun currentStatus(): TaskStatus
}