package net.sr89.haystacker.server.api

import com.fasterxml.jackson.annotation.JsonProperty
import net.sr89.haystacker.server.JacksonModule.auto
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.lens.string

data class SearchResult(@JsonProperty("path") val path: String)

data class Index(@JsonProperty("location") val location: String)

data class ListIndexesResponse(@JsonProperty("indexes") val indexes: List<Index>)

data class SearchResponse(
    @JsonProperty("totalResults") val totalResults: Long,
    @JsonProperty("returnedResults") val returnedResults: Int,
    @JsonProperty("results") val results: List<SearchResult>
)

data class BackgroundTaskStatusResponse(
    @JsonProperty("taskId") val taskId: String,
    @JsonProperty("status") val status: String,
    @JsonProperty("description") val description: String
)

data class TaskIdResponse(
    @JsonProperty("taskId") val taskId: String
)

data class TaskInterruptResponse(
    /**
     * True if the task was running and was sent an interrupt, false otherwise.
     */
    @JsonProperty("interruptSent") val interruptSent: Boolean
)

val hslStringQuery = Query.string().required("hslQuery")
val indexPathQuery = Query.string().required("indexPath")
val taskIdQuery = Query.string().required("taskId")
val directoryQuery = Query.string().required("directory")
val maxResultsQuery = Query.int().optional("maxResults")

val stringBody = Body.string(ContentType.TEXT_PLAIN).toLens()
val taskIdResponse = Body.auto<TaskIdResponse>().toLens()
val backgroundTaskStatusResponse = Body.auto<BackgroundTaskStatusResponse>().toLens()
val interruptBackgroundTaskResponse = Body.auto<TaskInterruptResponse>().toLens()
val searchResponse = Body.auto<SearchResponse>().toLens()
val listIndexesResponse = Body.auto<ListIndexesResponse>().toLens()