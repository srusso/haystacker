package net.sr89.haystacker.server.handlers

import net.sr89.haystacker.index.IndexManagerProvider
import net.sr89.haystacker.lang.parser.HslParser
import net.sr89.haystacker.server.api.hslQuery
import net.sr89.haystacker.server.api.indexPath
import net.sr89.haystacker.server.api.maxResults
import net.sr89.haystacker.server.api.searchResponse
import net.sr89.haystacker.server.api.stringBody
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import java.nio.file.Paths

private val hslParser = HslParser()

class SearchHandler(private val indexManagerProvider: IndexManagerProvider) : HttpHandler {
    override fun invoke(request: Request): Response {
        val hslQuery: String = hslQuery(request)

        val parsedQuery = hslParser.parse(hslQuery)
        val maxResults: Int = maxResults(request) ?: 10
        val indexPath: String = indexPath(request)
        val indexManager = indexManagerProvider.forPath(indexPath)

        println("Received request to search '$hslQuery' on index $indexPath, returning a maximum of $maxResults results")

        return if (!Paths.get(indexPath).toFile().exists()) {
            Response(Status.NOT_FOUND).with(stringBody of "Index at $indexPath not found")
        } else {
            val hits = indexManager.search(parsedQuery, maxResults)

            Response(Status.OK).with(searchResponse of hits)
        }
    }
}