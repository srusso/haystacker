package net.sr89.haystacker.server

import net.sr89.haystacker.index.IndexManager
import net.sr89.haystacker.index.IndexManagerProvider
import net.sr89.haystacker.server.config.ServerConfig
import net.sr89.haystacker.server.config.SettingsManager
import net.sr89.haystacker.server.handlers.QuitHandler
import org.http4k.server.Http4kServer
import org.kodein.di.DI
import org.kodein.di.instance
import org.kodein.di.newInstance
import java.nio.file.Path
import java.nio.file.Paths

class HaystackerApplication(
    private val restServer: Http4kServer,
    private val indexManagerProvider: IndexManagerProvider,
    private val settingsManager: SettingsManager
) {
    fun run() {
        println("Starting REST server")

        restServer.start()

        println("Setting up filesystem watchers for existing indexes")

        settingsManager.indexes()
            .map(indexManagerProvider::forPath)
            .forEach(IndexManager::startWatchingFileSystemChanges)

        println("Haystacker REST server started")
    }

    companion object {
        fun server(di: DI, settingsDirectory: Path, port: Int): HaystackerApplication {
            val config = ServerConfig(port, settingsDirectory)

            val serverInstance: Http4kServer by di.instance(arg = config)

            val hslServer by di.newInstance {
                HaystackerApplication(
                    restServer = serverInstance,
                    indexManagerProvider = instance(),
                    settingsManager = instance(arg = config)
                )
            }

            val quitHandler: QuitHandler by di.instance(arg = config)

            quitHandler.serverInstance = serverInstance

            return hslServer
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val settingsDirectory = if (args.isEmpty()) {
                println("Using Haystacker executable directory (${Paths.get(".").toAbsolutePath()}) as settings directory")
                Paths.get(".")
            } else {
                println("Using settings directory ${args[0]}")
                Paths.get(args[0])
            }

            server(serverDI(), settingsDirectory, 9000).run()
        }
    }
}