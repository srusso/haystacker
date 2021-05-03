package net.sr89.haystacker.server.handlers

import net.sr89.haystacker.server.api.Index
import net.sr89.haystacker.server.api.ListIndexesResponse
import net.sr89.haystacker.server.api.listIndexesResponse
import net.sr89.haystacker.server.config.SettingsManager
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with

class ListIndexesHandler(val settingsManager: SettingsManager) : HttpHandler {
    override fun invoke(request: Request): Response {
        val indexes = ListIndexesResponse(settingsManager.indexes().map { indexPath -> Index(indexPath) }.toList())
        return Response(Status.OK).with(listIndexesResponse of indexes)
    }
}