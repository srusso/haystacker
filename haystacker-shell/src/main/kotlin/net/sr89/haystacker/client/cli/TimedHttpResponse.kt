package net.sr89.haystacker.client.cli

import org.http4k.core.Body
import org.http4k.core.Headers
import org.http4k.core.Response
import org.http4k.core.Status
import java.io.InputStream
import java.time.Duration

class TimedHttpResponse(val response: Response, val duration: Duration) : Response {
    override val body: Body
        get() = response.body
    override val headers: Headers
        get() = response.headers
    override val status: Status
        get() = response.status
    override val version: String
        get() = response.version

    override fun body(body: InputStream, length: Long?) = response.body(body, length)

    override fun body(body: String) = response.body(body)

    override fun body(body: Body) = response

    override fun header(name: String, value: String?) = response.header(name, value)

    override fun headers(headers: Headers) = response.headers(headers)

    override fun removeHeader(name: String) = response.removeHeader(name)

    override fun replaceHeader(name: String, value: String?) = response.replaceHeader(name, value)

    override fun replaceHeaders(source: Headers) = response.replaceHeaders(source)

    override fun status(new: Status) = response.status(new)
}