package net.sr89.haystacker.server.api

import com.fasterxml.jackson.core.type.TypeReference
import org.http4k.core.HttpHandler
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import java.io.IOException
import java.net.SocketException
import java.time.Duration

class HaystackerRestClient(val baseUrl: String, val underlyingClient: HttpHandler) {
    fun createIndex(indexPath: String): TimedHttpResponse<String> {
        val request = Request(POST, "$baseUrl/index")
            .with(indexPathQuery of indexPath)

        return executeTimed(request, object: TypeReference<String>() {})
    }

    fun listIndexes(): TimedHttpResponse<ListIndexesResponse> {
        val request = Request(GET, "$baseUrl/index")

        return executeTimed(request, object: TypeReference<ListIndexesResponse>() {})
    }

    fun listVolumes(): TimedHttpResponse<ListVolumesResponse> {
        val request = Request(GET, "$baseUrl/volume")

        return executeTimed(request, object: TypeReference<ListVolumesResponse>() {})
    }

    fun indexDirectory(indexPath: String, dirPath: String): TimedHttpResponse<TaskIdResponse> {
        val request = Request(POST, "$baseUrl/directory")
            .with(
                indexPathQuery of indexPath,
                directoryQuery of dirPath
            )

        return executeTimed(request, object: TypeReference<TaskIdResponse>() {})
    }

    fun deindexDirectory(indexPath: String, dirPath: String): TimedHttpResponse<TaskIdResponse> {
        val request = Request(DELETE, "$baseUrl/directory")
            .with(
                indexPathQuery of indexPath,
                directoryQuery of dirPath
            )

        return executeTimed(request, object: TypeReference<TaskIdResponse>() {})
    }

    fun taskStatus(taskId: String): TimedHttpResponse<BackgroundTaskStatusResponse> {
        val request = Request(GET, "$baseUrl/task")
            .with(taskIdQuery of taskId)

        return executeTimed(request, object: TypeReference<BackgroundTaskStatusResponse>() {})
    }

    fun taskInterrupt(taskId: String): TimedHttpResponse<TaskInterruptResponse> {
        val request = Request(POST, "$baseUrl/task/interrupt")
            .with(taskIdQuery of taskId)

        return executeTimed(request, object: TypeReference<TaskInterruptResponse>() {})
    }

    fun shutdownServer(): TimedHttpResponse<String> {
        return executeTimed(Request(POST, "$baseUrl/quit"), object: TypeReference<String>() {})
    }

    fun search(query: String, maxResults: Int, indexPath: String): TimedHttpResponse<SearchResponse> {
        val request = Request(POST, "$baseUrl/search")
            .with(
                hslStringQuery of query,
                indexPathQuery of indexPath,
                maxResultsQuery of maxResults
            )

        return executeTimed(request, object: TypeReference<SearchResponse>() {})
    }

    private fun <T> executeTimed(request: Request, typeReference: TypeReference<T>): TimedHttpResponse<T> {
        val start = System.currentTimeMillis()
        return try {
            val response = underlyingClient(request)
            return if (response.status == Status.CONNECTION_REFUSED) {
                couldNotConnectError(start, typeReference)
            } else {
                TimedHttpResponse(response, typeReference, durationSince(start))
            }
        } catch (e: SocketException) {
            couldNotConnectError(start, typeReference)
        } catch (e: IOException) {
            couldNotConnectError(start, typeReference)
        }
    }

    private fun <T> couldNotConnectError(start: Long, typeReference: TypeReference<T>): TimedHttpResponse<T> {
        val response = Response(Status.CONNECTION_REFUSED)
            .with(stringBody of "Could not connect to $baseUrl. Is the server running at the specified address?")
        return TimedHttpResponse(response, typeReference, durationSince(start))
    }

    private fun durationSince(start: Long) = Duration.ofMillis(System.currentTimeMillis() - start)
}