package net.sr89.haystacker.ui.search

import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import mu.KotlinLogging
import net.sr89.haystacker.server.api.HaystackerRestClient
import net.sr89.haystacker.ui.uicomponents.IndexDropdownManager
import net.sr89.haystacker.ui.uicomponents.model.IndexDropdownEntry
import net.sr89.haystacker.ui.uicomponents.model.UISearchResult
import org.http4k.core.Status
import org.springframework.util.unit.DataSize
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ExecutorService

class SearchManager(
    private val indexDropdownManager: IndexDropdownManager,
    private val restClient: HaystackerRestClient,
    private val executor: ExecutorService
) {
    private val logger = KotlinLogging.logger {}

    private val minDurationBetweenSearches = Duration.ofMillis(50)

    val searchResults: ObservableList<UISearchResult> = FXCollections.observableArrayList()

    var lastSearchTimestamp = System.nanoTime()

    val searchTestBox = TextField()
    val searchBoxPanel = searchBoxPanel()

    init {
        indexDropdownManager.indexDropdown
            .valueProperty()
            // execute the current search when a new index is selected
            .addListener { _, _, _ -> onSimpleSearchUpdate() }
    }

    private fun searchBoxPanel(): Pane {
        val searchLabel = Label("Search")
        searchTestBox.promptText = "type file name.."
        searchTestBox.onKeyTyped = EventHandler {
            onSimpleSearchUpdate()
        }
        searchTestBox.requestFocus()

        val advancedSearchToggle = CheckBox("Advanced search")

        val rightHBox = HBox(10.0) // TODO: re-add the advanced search toggle here in issue 62
        HBox.setHgrow(rightHBox, Priority.ALWAYS)
        rightHBox.alignment = Pos.CENTER_RIGHT

        val hbox = HBox(10.0, searchLabel, searchTestBox, rightHBox)
        hbox.alignment = Pos.CENTER_LEFT
        VBox.setMargin(hbox, Insets(10.0))

        return hbox
    }

    /**
     * Called when using simple search mode (i.e. based on file name only, as opposed to full blown HSL).
     */
    fun onSimpleSearchUpdate() {
        if (searchTestBox.length < 3) {
            return
        }

        val selectedIndex = indexDropdownManager.selectedIndex()

        if (selectedIndex == null) {
            logger.warn { "No index selected!" }
            Platform.runLater { searchResults.clear() }
            return
        }

        executor.submit { executeSearch(searchTestBox, selectedIndex) }
    }

    private fun executeSearch(searchTestBox: TextField, selectedIndex: IndexDropdownEntry) {
        val nano = System.nanoTime()

        if (Duration.ofNanos(nano - lastSearchTimestamp) < minDurationBetweenSearches) {
            return
        }

        lastSearchTimestamp = nano

        val query = searchTestBox.text
        logger.info { "searching $searchTestBox ..." }

        val response = restClient.search(
            generateHslFromFilenameQuery(query),
            100,
            selectedIndex.indexPath
        )

        if (response.status != Status.OK) {
            logger.info { "Search error: ${response.rawBody()}" }
        } else {
            val (totalResults, returnedResultsCount, results) = response.responseBody()
            logger.info { "Results: $totalResults" }

            if (query != searchTestBox.text) {
                return
            }

            Platform.runLater {
                searchResults.clear()
                searchResults.addAll(
                    results.map { res ->
                        UISearchResult(
                            res.path,
                            DataSize.ofMegabytes(2),
                            Instant.now(),
                            Instant.now()
                        )
                    }
                )
            }
        }
    }

    private fun generateHslFromFilenameQuery(filenameQuery: String): String {
        return "name = \"$filenameQuery\""
    }
}