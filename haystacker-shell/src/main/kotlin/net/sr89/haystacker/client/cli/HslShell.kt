package net.sr89.haystacker.client.cli

import net.sr89.haystacker.server.JacksonModule
import net.sr89.haystacker.server.api.SearchResponse
import net.sr89.haystacker.server.api.directory
import net.sr89.haystacker.server.api.hslQuery
import net.sr89.haystacker.server.api.indexPath
import net.sr89.haystacker.server.api.maxResults
import net.sr89.haystacker.server.api.taskId
import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.with
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.shell.Bootstrap
import org.springframework.shell.core.CommandMarker
import org.springframework.shell.core.annotation.CliCommand
import org.springframework.shell.core.annotation.CliOption
import org.springframework.stereotype.Component


@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class HslShell : CommandMarker {
    private val noIndexSetErrorMessage = "Please set the current index with 'set-index'"

    private val httpClient = ApacheClient()

    // TODO use config for this
    private val baseUrl = "http://localhost:9000"

    private var currentIndex: String? = null

    @CliCommand(value = ["set-index"], help = "Sets the current index")
    fun setCurrentIndex(@CliOption(key = [""], help = "The index to use (server's filesystem)") index: String): String {
        this.currentIndex = index
        return "Set current index to: $index"
    }

    @CliCommand(value = ["current-index"], help = "Shows the current index")
    fun showCurrentIndex(): String {
        return if (currentIndex != null) {
            "Current index: $currentIndex"
        } else {
            "No current index set. Please use the 'set-index' command."
        }
    }

    @CliCommand(value = ["create-index"], help = "Create a new index in the server machine")
    fun createIndex(@CliOption(key = [""], help = "Where to create the new index, on the server's filesystem") path: String): String {
        val createRequest = Request(Method.POST, "$baseUrl/index")
            .with(indexPath of path)

        val response = httpClient(createRequest)

        return if (response.status == Status.OK) {
            "Created index at $path"
        } else {
            "Error:\n${response.bodyString()}"
        }
    }

    @CliCommand(value = ["add-to-index"], help = "Add a directory and its contents to the index")
    fun indexDirectory(
        @CliOption(key = [""], help = "The directory to add to the index") dirPath: String
    ): String {
        val ci = currentIndex ?: return noIndexSetErrorMessage

        val indexRequest = Request(Method.POST, "$baseUrl/directory")
            .with(indexPath of ci, directory of dirPath)

        val response = executeTimed(indexRequest)

        return if (response.status == Status.OK) {
            "Started task to add $dirPath to index $ci: ${response.bodyString()}" +
                "\nTook: ${response.duration.toMillis()} ms"
        } else {
            "Error:\n${response.bodyString()}"
        }
    }

    @CliCommand(value = ["deindex"], help = "Remove a directory and its contents from the index")
    fun deindexDirectory(
        @CliOption(key = [""], help = "The directory to remove from the index") dirPath: String
    ): String {
        val ci = currentIndex ?: return noIndexSetErrorMessage

        val deindexRequest = Request(Method.DELETE, "$baseUrl/directory")
            .with(indexPath of ci, directory of dirPath)

        val response = executeTimed(deindexRequest)

        return if (response.status == Status.OK) {
            "Removed $dirPath to index $ci" +
                "\nTook: ${response.duration.toMillis()} ms"
        } else {
            "Error:\n${response.bodyString()}"
        }
    }

    @CliCommand(value = ["task-status"], help = "Check the long-running task status")
    fun taskStatus(
        @CliOption(key = [""], help = "The task ID") taskIdParam: String
    ): String {
        val taskStatusRequest = Request(Method.GET, "$baseUrl/task")
            .with(taskId of taskIdParam)

        val response = executeTimed(taskStatusRequest)

        return if (response.status == Status.OK) {
            response.response.bodyString()
        } else {
            "Error:\n${response.bodyString()}"
        }
    }

    @CliCommand(value = ["task-interrupt"], help = "Sends interrupt signal to running task")
    fun taskInterrupt(
        @CliOption(key = [""], help = "The task ID") taskIdParam: String
    ): String {
        val taskStatusRequest = Request(Method.POST, "$baseUrl/task/interrupt")
            .with(taskId of taskIdParam)

        val response = executeTimed(taskStatusRequest)

        return if (response.status == Status.OK) {
            response.response.bodyString()
        } else {
            "Error:\n${response.bodyString()}"
        }
    }

    @CliCommand(value = ["server-shutdown"], help = "Shutdown the server")
    fun shutdownServer(): String {
        val response = executeTimed(Request(Method.POST, "$baseUrl/quit"))

        return if (response.status == Status.OK) {
            "Sent shutdown request to $baseUrl."
        } else {
            "Error stopping server at $baseUrl: \n${response.bodyString()}"
        }
    }

    @CliCommand(value = ["search"], help = "Search the current index using HSL (Haystacker Search Language)")
    fun searchIndex(
        @CliOption(key = [""], help = "The HSL query") hsl: String,
        @CliOption(key = ["max-results", "mr"], mandatory = false, specifiedDefaultValue = "10", unspecifiedDefaultValue = "10") max: Int
    ): String {
        val ci = currentIndex ?: return noIndexSetErrorMessage

        val searchRequest = Request(Method.POST, "$baseUrl/search")
            .with(hslQuery of hsl,
                indexPath of ci,
                maxResults of max
            )

        val response = executeTimed(searchRequest)

        return if (response.status == Status.OK) {
            val searchResponse = JacksonModule.asA(response.bodyString(), SearchResponse::class)
            "Total results: ${searchResponse.totalResults}\n" +
                "Returned results: ${searchResponse.returnedResults}\n" +
                "Items:\n" +
                searchResponse.results.joinToString("\n") { result -> "Path: ${result.path}" } +
                "\nTook: ${response.duration.toMillis()} ms"
        } else {
            "Could not search $ci: \n${response.bodyString()}"
        }
    }


}

fun main(args: Array<String>) {
    Bootstrap.main(args)
}