package net.sr89.haystacker.client

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