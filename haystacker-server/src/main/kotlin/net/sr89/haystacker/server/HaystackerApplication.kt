package net.sr89.haystacker.server

import net.sr89.haystacker.index.IndexManager
import net.sr89.haystacker.index.IndexManagerProvider
import net.sr89.haystacker.server.cmdline.getPortOrDefault
import net.sr89.haystacker.server.cmdline.getSettingsDirectory
import net.sr89.haystacker.server.config.ServerConfig
import net.sr89.haystacker.server.config.SettingsManager
import net.sr89.haystacker.server.handlers.QuitHandler
import org.http4k.server.Http4kServer
import org.kodein.di.DI
import org.kodein.di.instance
import org.kodein.di.newInstance
import java.nio.file.Paths

class HaystackerApplication(
    private val restServer: Http4kServer,
    private val indexManagerProvider: IndexManagerProvider,
    private val settingsManager: SettingsManager
) {
    fun run() {
        println("Using settings directory '${settingsManager.config.settingsDirectory}'")

        println("Setting up filesystem watchers for existing indexes")

        settingsManager.indexes()
            .map(indexManagerProvider::forPath)
            .forEach(IndexManager::startWatchingFileSystemChanges)

        println("Starting REST server on port ${restServer.port()}")

        restServer.start()

        println("Haystacker REST server started on port ${restServer.port()}")
    }

    companion object {
        fun application(di: DI): HaystackerApplication {
            val application by di.newInstance {
                HaystackerApplication(
                    restServer = instance(),
                    indexManagerProvider = instance(),
                    settingsManager = instance()
                )
            }

            val quitHandler: QuitHandler by di.instance()

            // ugly circular dependency
            quitHandler.serverInstance = application.restServer

            return application
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val settingsDirectory = Paths.get(args.getSettingsDirectory())
            val port = args.getPortOrDefault()

            application(applicationModule(ServerConfig(port, settingsDirectory))).run()
        }
    }
}