package net.sr89.haystacker.ui.uicomponents

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.ChoiceBox
import javafx.util.StringConverter
import mu.KotlinLogging
import net.sr89.haystacker.server.api.HaystackerRestClient
import net.sr89.haystacker.server.async.task.BackgroundTask
import net.sr89.haystacker.server.async.task.BackgroundTaskManager
import net.sr89.haystacker.server.async.task.TaskExecutionState.RUNNING
import net.sr89.haystacker.server.async.task.TaskStatus
import net.sr89.haystacker.ui.uicomponents.model.IndexDropdownEntry
import org.http4k.core.Status
import java.time.Duration

class IndexDropdownManager(
    private val restClient: HaystackerRestClient,
    private val backgroundTaskManager: BackgroundTaskManager
) {
    private val logger = KotlinLogging.logger {}

    private val none = IndexDropdownEntry("-")

    private val indexList: ObservableList<IndexDropdownEntry> = FXCollections.observableArrayList(none)

    val indexDropdown = indexDropdown()

    fun start() {
        backgroundTaskManager.submitEternally(object : BackgroundTask {
            override fun run() = updateIndexList()

            override fun interrupt() = false

            override fun currentStatus() = TaskStatus(RUNNING, "Keeping index list in sync with server")
        }, Duration.ofSeconds(3))
    }

    fun selectedIndex(): IndexDropdownEntry? {
        val selectedValue = indexDropdown.value
        return if (selectedValue == none) null else selectedValue
    }

    private fun indexDropdown(): ChoiceBox<IndexDropdownEntry> {
        val indexDropdown = ChoiceBox<IndexDropdownEntry>()
        indexDropdown.items = indexList
        indexDropdown.value = none
        indexDropdown.converter = object : StringConverter<IndexDropdownEntry>() {
            override fun toString(entry: IndexDropdownEntry) = entry.indexPath

            override fun fromString(indexPath: String) = IndexDropdownEntry(indexPath)
        }
        return indexDropdown
    }

    private fun updateIndexList() {
        val indexResponse = restClient.listIndexes()

        if (indexResponse.status == Status.OK) {
            val upToDateIndexes = indexResponse.responseBody().indexes
                .map { i -> IndexDropdownEntry(i.location) }
                .sortedByDescending { i -> i.indexPath }
                .plus(none)

            if (indexList != upToDateIndexes) {
                val currentSelection = indexDropdown.value
                indexList.setAll(upToDateIndexes)
                indexDropdown.value = when {
                    currentSelection == none -> indexList[0]
                    indexList.contains(currentSelection) -> currentSelection
                    else -> none
                }
            }
        } else {
            logger.warn { "Unable to fetch index list from server (${indexResponse.status.code}): ${indexResponse.rawBody()}" }
        }
    }
}