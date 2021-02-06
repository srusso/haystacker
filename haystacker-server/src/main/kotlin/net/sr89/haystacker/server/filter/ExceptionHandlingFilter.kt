package net.sr89.haystacker.server.filter

import net.sr89.haystacker.lang.exception.InvalidHslGrammarException
import net.sr89.haystacker.server.api.stringBody
import org.http4k.core.Filter
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with

fun exceptionHandlingFilter() = Filter { next ->
    {
        try {
            next(it)
        } catch (e: InvalidHslGrammarException) {
            Response(Status.BAD_REQUEST).with(stringBody of "Unable to parse query ${e.hslQuery}: Invalid HSL query. Refer to the README for a guide: https://github.com/srusso/haystacker")
        } catch (e: Exception) {
            Response(Status.NOT_FOUND).with(stringBody of e.message!!)
        }
    }
}