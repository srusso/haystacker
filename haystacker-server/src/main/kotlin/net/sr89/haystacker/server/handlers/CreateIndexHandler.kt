package net.sr89.haystacker.server.handlers

import mu.KotlinLogging
import net.sr89.haystacker.index.IndexManagerProvider
import net.sr89.haystacker.server.api.indexPathQuery
import net.sr89.haystacker.server.config.SettingsManager
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status

class CreateIndexHandler(val indexManagerProvider: IndexManagerProvider, val settingsManager: SettingsManager) :
    HttpHandler {
    private val logger = KotlinLogging.logger {}

    override fun invoke(request: Request): Response {
        val indexPath: String = indexPathQuery(request)
        val indexManager = indexManagerProvider.forPath(indexPath)

        logger.info { "Received request to create index at $indexPath" }

        settingsManager.addIndex(indexPath)

        indexManager.createNewIndex()
        indexManager.startWatchingFileSystemChanges()

        return Response(Status.OK)
    }
}