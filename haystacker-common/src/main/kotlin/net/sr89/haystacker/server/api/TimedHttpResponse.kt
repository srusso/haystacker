package net.sr89.haystacker.server.api

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.http4k.core.Response
import org.http4k.core.Status
import java.time.Duration

private val mapper = ObjectMapper()

class TimedHttpResponse<T>(
    private val response: Response,
    private val typeReference: TypeReference<T>,
    val duration: Duration
) {
    val status: Status
        get() = response.status

    fun responseBody(): T = mapper.readValue(response.bodyString(), typeReference)

    fun rawBody(): String = response.bodyString()
}