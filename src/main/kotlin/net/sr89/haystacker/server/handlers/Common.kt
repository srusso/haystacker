package net.sr89.haystacker.server.handlers

import net.sr89.haystacker.server.JacksonModule.auto
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.lens.string

data class SearchResult(val path: String)

data class SearchResponse(
    val totalResults: Long,
    val returnedResults: Int,
    val results: List<SearchResult>
)

val hslQuery = Query.string().required("hslQuery")
val indexPath = Query.string().required("indexPath")
val directory = Query.string().required("directory")
val maxResults = Query.int().optional("maxResults")
val stringBody = Body.string(ContentType.TEXT_PLAIN).toLens()
val searchResponse = Body.auto<SearchResponse>().toLens()