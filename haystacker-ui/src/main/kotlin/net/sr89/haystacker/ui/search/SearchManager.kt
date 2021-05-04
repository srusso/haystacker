package net.sr89.haystacker.ui.search

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import mu.KotlinLogging
import net.sr89.haystacker.server.api.HaystackerRestClient
import org.http4k.client.ApacheClient
import org.http4k.core.Status

class SearchManager {
    private val logger = KotlinLogging.logger {}

    // TODO make this configurable - this should be a KODEIN app
    private val restClient = HaystackerRestClient("http://localhost:9000", ApacheClient())
    val actualResults: ObservableList<String> = FXCollections.observableArrayList()

    /**
     * Called when using simple search mode (i.e. based on file name only, as opposed to full blown HSL).
     */
    fun onSimpleSearchUpdate(filenameQuery: String) { //TODO this probably needs to be a class containing query, dates, etc.
        if (filenameQuery.length < 3) {
            return
        }

        logger.info { "searching $filenameQuery ..." }

        val response = restClient.search(
            generateHslFromFilenameQuery(filenameQuery),
            100,
            "D:\\index"
        )

        if (response.status != Status.OK) {
            logger.info { "Search error: ${response.rawBody()}" }
        } else {
            val (totalResults, returnedResults, results) = response.responseBody()
            logger.info { "Results: $totalResults" }

            actualResults.clear()
            actualResults.addAll(
                results.map { res -> res.path }
            )
        }
    }

    private fun generateHslFromFilenameQuery(filenameQuery: String): String {
        return "name = \"$filenameQuery\""
    }
}