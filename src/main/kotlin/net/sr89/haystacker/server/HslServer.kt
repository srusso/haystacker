package net.sr89.haystacker.server

import net.sr89.haystacker.index.IndexManager
import net.sr89.haystacker.lang.parser.HslParser
import net.sr89.haystacker.lang.translate.HslToLucene
import net.sr89.haystacker.server.JacksonModule.auto
import org.apache.lucene.search.ScoreDoc
import org.http4k.client.ApacheClient
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Jetty
import org.http4k.server.asServer
import java.nio.file.Files

data class SearchResponse(val totalResults: Long)

val hslQuery = Query.string().required("hslQuery")
val indexPath = Query.string().required("indexPath")
val maxResults = Query.int().optional("maxResults")

val hslToLucene = HslToLucene(HslParser())

private fun searchHandler(): HttpHandler {
    return { request: Request ->
        val hslQuery: String = hslQuery(request)
        val parsedQuery = hslToLucene.toLuceneQuery(hslQuery)
        val maxResults: Int = maxResults(request) ?: 10
        val indexPath: String = indexPath(request)
        val indexManager = IndexManager(indexPath)

        println("Received request to search '$hslQuery' on index $indexPath, returning a maximum of $maxResults results")

        val hits = indexManager.searchIndex(parsedQuery)

        hits.scoreDocs.map(ScoreDoc::doc).mapNotNull(indexManager::fetchDocument).toList()
            .map { document -> document.getField("path") }

        Response(OK).with(
            Body.auto<SearchResponse>().toLens() of SearchResponse(hits.totalHits.value)
        )
    }
}

private fun createServer(): HttpHandler {
    return routes(
        "search" bind POST to searchHandler()
    )
}

/**
 * Start a background process/daemon that listens for commands.
 * Commands:
 * Perform HSL search on index
 * Add directory to index
 * Add directory recursively to index
 * Remove directory from index
 * Create new index
 */
fun main() {
    val app = ServerFilters.CatchLensFailure.then(createServer())

    val jettyServer = app.asServer(Jetty(9000)).start()

    val indexFile = Files.createTempDirectory("index").toFile()

    val request = Request(POST, "http://localhost:9000/search")
        .with(
            hslQuery of "name = haystacker-1.0-SNAPSHOT.jar",
            maxResults of 15,
            indexPath of indexFile.absolutePath
        )

    val client = ApacheClient()

    println(client(request))

    jettyServer.stop()
}