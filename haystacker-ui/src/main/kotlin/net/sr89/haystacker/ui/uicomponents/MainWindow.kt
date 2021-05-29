package net.sr89.haystacker.ui.uicomponents

import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.SimpleListProperty
import javafx.beans.value.ObservableValue
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TableColumn
import javafx.scene.control.TableRow
import javafx.scene.control.TableView
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.Stage
import javafx.util.Callback
import mu.KotlinLogging
import net.sr89.haystacker.ui.app.onUIAppClose
import net.sr89.haystacker.ui.search.SearchManager
import net.sr89.haystacker.ui.uicomponents.model.UISearchResult
import org.springframework.util.unit.DataSize
import java.awt.Desktop
import java.io.File
import java.time.Instant


/**
 * TODO
 * - Screen to add a new index
 * - Remove index button
 * - Button to add directory to an index. How to display task progress? Running tasks tab?
 * - Result sorting (by re-executing the search!)
 * - Switch to advanced search (HSL), which includes a link to the HSL guide
 * - Display server status in UI, with button to start/stop it?
 */

class MainWindow(
    private val searchManager: SearchManager,
    private val indexDropdownManager: IndexDropdownManager,
    val createArchiveWizard: CreateArchiveWizard
) {
    private val logger = KotlinLogging.logger {}

    fun buildStage(stage: Stage) {
        val vbox = VBox(10.0, searchManager.searchBoxPanel, resultsListView(), bottomControls())
        vbox.alignment = Pos.CENTER

        val scene = Scene(vbox, 480.0, 320.0)

        stage.scene = scene

        stage.title = "Haystacker"
        stage.show()

        indexDropdownManager.start()

        stage.onCloseRequest = EventHandler { onUIAppClose() }
    }

    private fun resultsListView(): Pane {
        val resultTable: TableView<UISearchResult> = TableView()

        val filename: TableColumn<UISearchResult, String> = TableColumn("File")
        val size: TableColumn<UISearchResult, DataSize> = TableColumn("Size")
        val created: TableColumn<UISearchResult, Instant> = TableColumn("Created")
        val lastModified: TableColumn<UISearchResult, Instant> = TableColumn("Modified")

        filename.cellValueFactory = SimpleCellValueFactory { p -> p.filename }
        size.cellValueFactory = SimpleCellValueFactory { p -> p.size }
        created.cellValueFactory = SimpleCellValueFactory { p -> p.created }
        lastModified.cellValueFactory = SimpleCellValueFactory { p -> p.lastModified }

        resultTable.columns.addAll(filename, size, created, lastModified)
        resultTable.itemsProperty().bind(SimpleListProperty(searchManager.searchResults))
        resultTable.rowFactory = resultRowFactory()

        resultTable.columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
        resultTable.isFocusTraversable = false
        val hbox = HBox(10.0, resultTable)
        hbox.padding = Insets(10.0)
        hbox.alignment = Pos.CENTER
        HBox.setHgrow(resultTable, Priority.ALWAYS)
        VBox.setVgrow(hbox, Priority.ALWAYS)
        return hbox
    }

    private fun resultRowFactory() = Callback<TableView<UISearchResult>, TableRow<UISearchResult>> {
        val row = TableRow<UISearchResult>()

        row.onMouseClicked = EventHandler { mouseEvent: MouseEvent ->
            if (mouseEvent.clickCount == 2 && !row.isEmpty) {
                val file = File(row.item.filename)
                try {
                    Desktop.getDesktop().open(file.parentFile)
                } catch (e: IllegalArgumentException) {
                    logger.warn { "Cannot navigate to directory ${file.parentFile}: ${e.message}" }
                }
            }
        }

        row
    }

    private fun bottomControls(): Pane {
        val indexLabel = Label("Archive")
        val createIndexButton = Button("Create")
        createIndexButton.onMouseClicked = EventHandler { createArchiveWizard.show() }
        val leftBox = HBox(10.0, indexLabel, indexDropdownManager.indexDropdown, createIndexButton)
        HBox.setHgrow(leftBox, Priority.NEVER)
        leftBox.alignment = Pos.CENTER_LEFT

        val closeButton = Button("Close")
        closeButton.onMouseClicked = EventHandler { onUIAppClose() }
        val rightHBox = HBox(10.0, closeButton)
        HBox.setHgrow(rightHBox, Priority.ALWAYS)
        rightHBox.alignment = Pos.CENTER_RIGHT

        val hbox = HBox(10.0, leftBox, rightHBox)
        hbox.alignment = Pos.BASELINE_RIGHT
        hbox.padding = Insets(0.0, 10.0, 10.0, 10.0)

        return hbox
    }
}

private class SimpleCellValueFactory<R>(val converter: (UISearchResult) -> R) :
    Callback<TableColumn.CellDataFeatures<UISearchResult, R>, ObservableValue<R>> {

    override fun call(param: TableColumn.CellDataFeatures<UISearchResult, R>) =
        ReadOnlyObjectWrapper(converter.invoke(param.value))
}