package net.sr89.haystacker.server.filter

import net.sr89.haystacker.lang.exception.InvalidHslDataSizeException
import net.sr89.haystacker.lang.exception.InvalidHslDateException
import net.sr89.haystacker.lang.exception.InvalidHslGrammarException
import net.sr89.haystacker.lang.exception.InvalidHslOperatorException
import net.sr89.haystacker.lang.exception.InvalidHslOrderByClause
import net.sr89.haystacker.lang.exception.SettingsUpdateException
import net.sr89.haystacker.server.InvalidTaskIdException
import net.sr89.haystacker.server.api.stringBody
import org.apache.lucene.index.IndexNotFoundException
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.with

class ExceptionHandler(val next: HttpHandler) : HttpHandler {
    override fun invoke(request: Request): Response {
        return try {
            next(request)
        } catch (e: InvalidHslGrammarException) {
            Response(BAD_REQUEST)
                .with(stringBody of "Unable to parse query ${e.hslQuery}: Invalid HSL query at (line = ${e.line}, column = ${e.column}). Refer to the README for a guide: https://github.com/srusso/haystacker")
        } catch (e: InvalidHslDateException) {
            Response(BAD_REQUEST)
                .with(stringBody of "Symbol '${e.symbol}' expects a date, but '${e.date}' is neither an \"ISO-8601/UTC date time\" (ex: 2011-12-03T10:15:30Z), nor an \"ISO-8601/date time with offset\" (ex: 2011-12-03T10:15:30+01:00) nor a date (ex: 2011-12-03)")
        } catch (e: InvalidHslOrderByClause) {
            Response(BAD_REQUEST)
                .with(stringBody of "Symbol '${e.symbol}' cannot be used in an \"order by\" clause")
        } catch (e: InvalidHslDataSizeException) {
            Response(BAD_REQUEST)
                .with(stringBody of "Symbol '${e.symbol}' expects a data-size value, but was '${e.dataSize}'")
        } catch (e: InvalidHslOperatorException) {
            Response(BAD_REQUEST)
                .with(stringBody of "Invalid operator (${e.operator}) for symbol '${e.symbol}' and value '${e.value}'")
        } catch (e: InvalidTaskIdException) {
            Response(BAD_REQUEST)
                .with(stringBody of "Invalid Task ID provided: ${e.taskId}")
        } catch (e: SettingsUpdateException) {
            Response(INTERNAL_SERVER_ERROR)
                .with(stringBody of "Request failed due to being enable to update settings file: ${e.message}")
        } catch (e: IndexNotFoundException) {
            Response(NOT_FOUND)
                .with(stringBody of "Index not found or corrupted")
        } catch (e: Exception) {
            Response(INTERNAL_SERVER_ERROR).with(stringBody of e.message!!)
        }
    }
}

class ExceptionHandlingFilter : Filter {
    override fun invoke(next: HttpHandler) = ExceptionHandler(next)
}
