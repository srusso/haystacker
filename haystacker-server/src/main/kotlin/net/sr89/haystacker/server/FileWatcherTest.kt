package net.sr89.haystacker.server

import com.sun.jna.platform.FileMonitor
import com.sun.jna.platform.FileMonitor.FILE_CREATED
import com.sun.jna.platform.FileMonitor.FILE_DELETED
import com.sun.jna.platform.FileMonitor.FILE_RENAMED
import com.sun.jna.platform.FileMonitor.FILE_SIZE_CHANGED
import java.io.File

object FileWatcherTest {
    private const val wantedEvents = FILE_CREATED or FILE_DELETED or FILE_RENAMED or FILE_SIZE_CHANGED

    @JvmStatic
    fun main(args: Array<String>) {
        val monitor = FileMonitor.getInstance()

        monitor.addWatch(File("C:"), wantedEvents)
        monitor.addFileListener {
            println("${it.file} ${it.type}")
        }


        Thread.sleep(1000000L)

    }
}