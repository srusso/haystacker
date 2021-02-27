package net.sr89.haystacker.async

interface DaemonProcess {
    fun interrupt()
    fun resume()
    fun running(): Boolean
}