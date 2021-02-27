package net.sr89.haystacker.async

interface BackgroundTask {
    fun run()
    fun interrupt()
    fun currentStatus(): TaskStatus
}