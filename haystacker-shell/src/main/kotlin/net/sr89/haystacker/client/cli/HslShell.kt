package net.sr89.haystacker.client.cli

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
    @CliCommand(value = ["web-get", "wg"])
    fun webGet(
        @CliOption(key = ["url"]) url: String): String {
        println("Lalalalala")
        return "result"
    }

}

fun main(args: Array<String>) {
    Bootstrap.main(args)
}