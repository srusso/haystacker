package net.sr89.haystacker.server

import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService

object HelloWorld {
    @JvmStatic
    fun main(args: Array<String>) {
        val watchService: WatchService = FileSystems.getDefault().newWatchService()

        Paths.get("C:")
            .register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)

        while (true) {
            val key: WatchKey = try {
                watchService.take()
            } catch (ex: InterruptedException) {
                return
            }

            println(key)

            for (event in key.pollEvents()) {
                val kind: WatchEvent.Kind<*> = event.kind()
                println("kind " + kind.name())
                val path = event.context() as Path
                println(path.toString())

            }

            key.reset()
        }
    }

}