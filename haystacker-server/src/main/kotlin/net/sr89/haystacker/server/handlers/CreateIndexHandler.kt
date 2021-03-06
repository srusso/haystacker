package net.sr89.haystacker.server.handlers

import net.sr89.haystacker.index.IndexManagerProvider
import net.sr89.haystacker.server.api.indexPath
import net.sr89.haystacker.server.config.SettingsManager
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status

class CreateIndexHandler(val indexManagerProvider: IndexManagerProvider, val settingsManager: SettingsManager): HttpHandler {
    override fun invoke(request: Request): Response {
        val indexPath: String = indexPath(request)
        val indexManager = indexManagerProvider.forPath(indexPath)

        println("Received request to create index at $indexPath")

        settingsManager.addIndex(indexPath)

        indexManager.createNewIndex()

        return Response(Status.OK)
    }
}