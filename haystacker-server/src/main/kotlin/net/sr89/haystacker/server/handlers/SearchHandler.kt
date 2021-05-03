package net.sr89.haystacker.server.handlers

import mu.KotlinLogging
import net.sr89.haystacker.index.IndexManagerProvider
import net.sr89.haystacker.lang.parser.HslParser
import net.sr89.haystacker.server.api.hslStringQuery
import net.sr89.haystacker.server.api.indexPathQuery
import net.sr89.haystacker.server.api.maxResultsQuery
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
    private val logger = KotlinLogging.logger {}

    override fun invoke(request: Request): Response {
        val hslQuery: String = hslStringQuery(request)

        val parsedQuery = hslParser.parse(hslQuery)
        val maxResults: Int = maxResultsQuery(request) ?: 10
        val indexPath: String = indexPathQuery(request)
        val indexManager = indexManagerProvider.forPath(indexPath)

        logger.info { "Received request to search '$hslQuery' on index $indexPath, returning a maximum of $maxResults results" }

        return if (!Paths.get(indexPath).toFile().exists()) {
            Response(Status.NOT_FOUND).with(stringBody of "Index at $indexPath not found")
        } else {
            val hits = indexManager.search(parsedQuery, maxResults)

            Response(Status.OK).with(searchResponse of hits)
        }
    }
}