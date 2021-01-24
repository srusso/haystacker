package net.sr89.haystacker.server.handlers

import com.fasterxml.jackson.annotation.JsonProperty
import net.sr89.haystacker.server.JacksonModule.auto
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.lens.string

data class SearchResult(@JsonProperty("path") val path: String)

data class SearchResponse(
    @JsonProperty("totalResults") val totalResults: Long,
    @JsonProperty("returnedResults") val returnedResults: Int,
    @JsonProperty("results") val results: List<SearchResult>
)

val hslQuery = Query.string().required("hslQuery")
val indexPath = Query.string().required("indexPath")
val directory = Query.string().required("directory")
val maxResults = Query.int().optional("maxResults")
val stringBody = Body.string(ContentType.TEXT_PLAIN).toLens()
val searchResponse = Body.auto<SearchResponse>().toLens()