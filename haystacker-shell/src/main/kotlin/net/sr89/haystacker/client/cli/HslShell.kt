package net.sr89.haystacker.client.cli

import net.sr89.haystacker.server.api.indexPath
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
    private val httpClient = ApacheClient()

    @CliCommand(value = ["index"])
    fun createIndex(@CliOption(key = ["index-path", "path"]) path: String): String {
        val createRequest = Request(Method.POST, "http://localhost:9000/index")
            .with(indexPath of path)

        val response = httpClient(createRequest)

        return if (response.status == Status.OK) {
            "Created index at $path"
        } else {
            "Error:\n${response.bodyString()}"
        }
    }
}

fun main(args: Array<String>) {
    Bootstrap.main(args)
}