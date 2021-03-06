package net.sr89.haystacker.server

import mu.KotlinLogging
import net.sr89.haystacker.index.IndexManager
import net.sr89.haystacker.index.IndexManagerProvider
import net.sr89.haystacker.server.cmdline.CmdLineArgs
import net.sr89.haystacker.server.config.ServerConfig
import net.sr89.haystacker.server.config.SettingsManager
import net.sr89.haystacker.server.handlers.QuitHandler
import org.http4k.server.Http4kServer
import org.kodein.di.DI
import org.kodein.di.instance
import org.kodein.di.newInstance
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

class HaystackerApplication(
    private val restServer: Http4kServer,
    private val indexManagerProvider: IndexManagerProvider,
    private val settingsManager: SettingsManager
) {
    fun run() {
        logger.info { "Using settings directory '${settingsManager.config.settingsDirectory}'" }

        logger.info { "Setting up filesystem watchers for existing indexes" }

        settingsManager.indexes()
            .map(indexManagerProvider::forPath)
            .forEach(IndexManager::startWatchingFileSystemChanges)

        logger.info { "Starting REST server on port ${restServer.port()}" }

        restServer.start()

        logger.info { "Haystacker REST server started on port ${restServer.port()}" }
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
            val commandLineArgs = CmdLineArgs(args)
            val settingsDirectory = Paths.get(commandLineArgs.settingsDirectory)
            val port = commandLineArgs.port

            application(applicationModule(ServerConfig(port, settingsDirectory))).run()
        }
    }
}