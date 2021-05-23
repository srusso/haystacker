package net.sr89.haystacker.ui.search

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.TextField
import mu.KotlinLogging
import net.sr89.haystacker.server.api.HaystackerRestClient
import net.sr89.haystacker.ui.uicomponents.UISearchResult
import org.http4k.core.Status
import org.springframework.util.unit.DataSize
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SearchManager(private val restClient: HaystackerRestClient) {
    private val logger = KotlinLogging.logger {}

    private val minDurationBetweenSearches = Duration.ofMillis(50)

    val searchResults: ObservableList<UISearchResult> = FXCollections.observableArrayList()

    val executor: ExecutorService = Executors.newCachedThreadPool()

    var lastSearchTimestamp = System.nanoTime()

    /**
     * Called when using simple search mode (i.e. based on file name only, as opposed to full blown HSL).
     */
    fun onSimpleSearchUpdate(filenameQuery: TextField) { //TODO this probably needs to be a class containing query, dates, etc.
        if (filenameQuery.length < 3) {
            return
        }

        val nano = System.nanoTime()

        if (Duration.ofNanos(nano - lastSearchTimestamp) < minDurationBetweenSearches) {
            return
        }

        lastSearchTimestamp = nano
        executor.submit { executeSearch(filenameQuery, nano) }
    }

    private fun executeSearch(filenameQuery: TextField, nano: Long) {
        val query = filenameQuery.text
        logger.info { "searching $filenameQuery ..." }

        val response = restClient.search(
            generateHslFromFilenameQuery(query),
            100,
            "D:\\index"
        )

        if (response.status != Status.OK) {
            logger.info { "Search error: ${response.rawBody()}" }
        } else {
            val (totalResults, returnedResultsCount, results) = response.responseBody()
            logger.info { "Results: $totalResults" }

            if (query != filenameQuery.text) {
                return
            }

            searchResults.clear()
            searchResults.addAll(
                results.map { res -> UISearchResult(res.path, DataSize.ofMegabytes(2), Instant.now(), Instant.now()) }
            )
        }
    }

    private fun generateHslFromFilenameQuery(filenameQuery: String): String {
        return "name = \"$filenameQuery\""
    }
}