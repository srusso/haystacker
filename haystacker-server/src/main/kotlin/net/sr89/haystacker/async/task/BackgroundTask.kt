package net.sr89.haystacker.async.task

interface BackgroundTask {
    fun run()
    fun interrupt()
    fun currentStatus(): TaskStatus
}