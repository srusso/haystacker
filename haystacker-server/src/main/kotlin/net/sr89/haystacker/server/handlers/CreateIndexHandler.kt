package net.sr89.haystacker.server.handlers

import net.sr89.haystacker.index.IndexManager
import net.sr89.haystacker.server.api.indexPath
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status

class CreateIndexHandler: HttpHandler {
    override fun invoke(request: Request): Response {
        val indexPath: String = indexPath(request)
        val indexManager = IndexManager.forPath(indexPath)

        println("Received request to create index at $indexPath")

        indexManager.createNewIndex().close()

        return Response(Status.OK)
    }
}