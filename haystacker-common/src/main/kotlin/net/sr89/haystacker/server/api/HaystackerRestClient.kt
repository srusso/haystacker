package net.sr89.haystacker.server.api

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import java.net.SocketException
import java.time.Duration

private val mapper = ObjectMapper()

class SearchResponseType : TypeReference<SearchResponse>()
class TaskCreatedResponseType : TypeReference<TaskIdResponse>()
class TaskStatusResponseType : TypeReference<BackgroundTaskStatusResponse>()

class HaystackerRestClient(val baseUrl: String, val underlyingClient: HttpHandler) {
    fun search(query: String, maxResults: Int, indexPath: String): SearchResponse {
        val request = Request(Method.POST, "$baseUrl/search")
            .with(
                hslQuery of query,
                net.sr89.haystacker.server.api.indexPath of indexPath,
                net.sr89.haystacker.server.api.maxResults of maxResults
            )

        return mapper.readValue(executeTimed(request).bodyString(), SearchResponseType())
    }

    private fun executeTimed(request: Request): TimedHttpResponse {
        val start = System.currentTimeMillis()
        return try {
            val response = underlyingClient(request)
            return if (response.status == Status.CONNECTION_REFUSED) {
                couldNotConnectError(start)
            } else {
                TimedHttpResponse(response, durationSince(start))
            }
        } catch (e: SocketException) {
            couldNotConnectError(start)
        }
    }

    private fun couldNotConnectError(start: Long): TimedHttpResponse {
        val response = Response(Status.CONNECTION_REFUSED)
            .with(stringBody of "Could not connect to $baseUrl. Is the server running at the specified address?")
        return TimedHttpResponse(response, durationSince(start))
    }

    private fun durationSince(start: Long) = Duration.ofMillis(System.currentTimeMillis() - start)
}