package net.sr89.haystacker.server.api

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

data class BackgroundTaskStatusResponse(
    @JsonProperty("taskId") val taskId: String,
    @JsonProperty("isRunning") val isRunning: Boolean
)

data class DirectoryIndexResponse(
    @JsonProperty("taskId") val taskId: String
)

val hslQuery = Query.string().required("hslQuery")

val indexPath = Query.string().required("indexPath")
val taskId = Query.string().required("taskId")
val directory = Query.string().required("directory")
val maxResults = Query.int().optional("maxResults")
val stringBody = Body.string(ContentType.TEXT_PLAIN).toLens()
val directoryIndexResponse = Body.auto<DirectoryIndexResponse>().toLens()
val backgroundTaskStatusResponse = Body.auto<BackgroundTaskStatusResponse>().toLens()
val searchResponse = Body.auto<SearchResponse>().toLens()