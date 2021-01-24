package net.sr89.haystacker.client.cli

import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with
import java.nio.file.Path


enum class Outcome { FAILURE, SUCCESS }

enum class DirectoryRequestType { RECURSIVELY, ONLY_SUBFILES }

data class HslServerResponse(val outcome: Outcome, val response: String)

sealed class HslServerRequest

data class AddDirectoryToIndex(val directoryRequestType: DirectoryRequestType, val directory: Path, val index: Path)
data class RemoveDirectoryDirectoryToIndex(val directoryRequestType: DirectoryRequestType, val directory: Path, val index: Path)
data class SearchIndex(val hslQuery: String, val index: Path)
data class CreateIndex(val index: Path)

fun sendRequestToServer(request: HslServerRequest): HslServerResponse {
    TODO("Send request to the HslServer")
}

fun main(args: Array<String>) {
    val port = 9000

    println("Started CLI client at port $port")

    val createRequest = Request(Method.POST, "http://localhost:9000/index")
        .with(
//            indexPath of TODO("Take this from the command line..")
        )

    val httpClient = ApacheClient()

    println(httpClient(createRequest))

    TODO("Implement CLI application that parses commands and makes requests to the server")
}