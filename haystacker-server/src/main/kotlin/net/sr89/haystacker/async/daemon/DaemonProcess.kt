package net.sr89.haystacker.async.daemon

interface DaemonProcess {
    fun interrupt()
    fun resume()
    fun running(): Boolean
}