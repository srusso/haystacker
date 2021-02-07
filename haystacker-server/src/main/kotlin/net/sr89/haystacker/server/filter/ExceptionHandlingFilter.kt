package net.sr89.haystacker.server.filter

import net.sr89.haystacker.lang.exception.InvalidHslDateException
import net.sr89.haystacker.lang.exception.InvalidHslGrammarException
import net.sr89.haystacker.lang.exception.InvalidSemanticException
import net.sr89.haystacker.server.api.stringBody
import org.http4k.core.Filter
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.with

fun exceptionHandlingFilter() = Filter { next ->
    {
        try {
            next(it)
        } catch (e: InvalidHslGrammarException) {
            Response(BAD_REQUEST)
                .with(stringBody of "Unable to parse query ${e.hslQuery}: Invalid HSL query at (line = ${e.line}, column = ${e.column}). Refer to the README for a guide: https://github.com/srusso/haystacker")
        } catch (e: InvalidSemanticException) {
            Response(BAD_REQUEST)
                .with(stringBody of e.message!!)
        } catch (e: InvalidHslDateException) {
            Response(BAD_REQUEST)
                .with(stringBody of "Symbol '${e.symbol}' expects a date, but '${e.date}' is neither an \"ISO-8601/UTC date time\" (ex: 2011-12-03T10:15:30Z), nor an \"ISO-8601/date time with offset\" (ex: 2011-12-03T10:15:30+01:00) nor a date (ex: 2011-12-03)")
        } catch (e: Exception) {
            Response(INTERNAL_SERVER_ERROR).with(stringBody of e.message!!)
        }
    }
}