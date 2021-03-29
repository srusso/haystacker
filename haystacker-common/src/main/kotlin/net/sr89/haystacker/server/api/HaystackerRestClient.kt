package net.sr89.haystacker.server.api

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import java.io.IOException
import java.net.SocketException
import java.time.Duration

class HaystackerRestClient(val baseUrl: String, val underlyingClient: HttpHandler) {
    fun createIndex(indexPath: String): TimedHttpResponse<String> {
        val request = Request(Method.POST, "$baseUrl/index")
            .with(net.sr89.haystacker.server.api.indexPath of indexPath)

        return executeTimed(request)
    }

    fun indexDirectory(indexPath: String, dirPath: String): TimedHttpResponse<TaskIdResponse> {
        val request = Request(Method.POST, "$baseUrl/directory")
            .with(
                net.sr89.haystacker.server.api.indexPath of indexPath,
                directory of dirPath
            )

        return executeTimed(request)
    }

    fun deindexDirectory(indexPath: String, dirPath: String): TimedHttpResponse<String> {
        val request = Request(Method.DELETE, "$baseUrl/directory")
            .with(
                net.sr89.haystacker.server.api.indexPath of indexPath,
                directory of dirPath
            )

        return executeTimed(request)
    }

    fun taskStatus(taskId: String): TimedHttpResponse<BackgroundTaskStatusResponse> {
        val request = Request(Method.GET, "$baseUrl/task")
            .with(net.sr89.haystacker.server.api.taskId of taskId)

        return executeTimed(request)
    }

    fun taskInterrupt(taskId: String): TimedHttpResponse<TaskInterruptResponse> {
        val request = Request(Method.POST, "$baseUrl/task/interrupt")
            .with(net.sr89.haystacker.server.api.taskId of taskId)

        return executeTimed(request)
    }

    fun shutdownServer(): TimedHttpResponse<String> {
        return executeTimed(Request(Method.POST, "$baseUrl/quit"))
    }

    fun search(query: String, maxResults: Int, indexPath: String): TimedHttpResponse<SearchResponse> {
        val request = Request(Method.POST, "$baseUrl/search")
            .with(
                hslQuery of query,
                net.sr89.haystacker.server.api.indexPath of indexPath,
                net.sr89.haystacker.server.api.maxResults of maxResults
            )

        return executeTimed(request)
    }

    private fun <T> executeTimed(request: Request): TimedHttpResponse<T> {
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
        } catch (e: IOException) {
            couldNotConnectError(start)
        }
    }

    private fun <T> couldNotConnectError(start: Long): TimedHttpResponse<T> {
        val response = Response(Status.CONNECTION_REFUSED)
            .with(stringBody of "Could not connect to $baseUrl. Is the server running at the specified address?")
        return TimedHttpResponse(response, durationSince(start))
    }

    private fun durationSince(start: Long) = Duration.ofMillis(System.currentTimeMillis() - start)
}