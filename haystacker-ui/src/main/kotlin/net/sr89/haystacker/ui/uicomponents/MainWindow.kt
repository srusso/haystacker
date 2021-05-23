package net.sr89.haystacker.ui.uicomponents

import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.SimpleListProperty
import javafx.beans.value.ObservableValue
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.ChoiceBox
import javafx.scene.control.Label
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.Stage
import javafx.util.Callback
import javafx.util.StringConverter
import net.sr89.haystacker.ui.search.SearchManager


/**
 * TODO
 * - Screen to add a new index
 * - Index selector (dropdown, included in the main window)
 * - Remove index button
 * - Button to add directory to an index. How to display task progress? Running tasks tab?
 * - Double click on a result opens windows explorer to it
 * - Result list displays created, last modified, size
 * - Result sorting
 * - Switch to advanced search (HSL), which includes a link to the HSL guide
 */

class MainWindow(private val searchManager: SearchManager) {
    fun buildStage(stage: Stage) {
        val vbox = VBox(10.0, searchBoxPanel(), resultsListView(), bottomControls())
        vbox.alignment = Pos.CENTER

        val scene = Scene(vbox, 480.0, 320.0)

        stage.scene = scene

        stage.title = "Haystacker"
        stage.show()
    }

    private fun searchBoxPanel(): Pane {
        val searchLabel = Label("Search")
        val searchTestBox = TextField()
        searchTestBox.promptText = "type file name.."
        searchTestBox.onKeyTyped = EventHandler {
            searchManager.onSimpleSearchUpdate(searchTestBox.text)
        }
        searchTestBox.requestFocus()

        val advancedSearchToggle = CheckBox("Advanced search")

        val rightHBox = HBox(10.0, advancedSearchToggle)
        HBox.setHgrow(rightHBox, Priority.ALWAYS)
        rightHBox.alignment = Pos.CENTER_RIGHT

        val hbox = HBox(10.0, searchLabel, searchTestBox, rightHBox)
        hbox.alignment = Pos.CENTER_LEFT
        VBox.setMargin(hbox, Insets(10.0))

        return hbox
    }

    private fun resultsListView(): Pane {
        val resultTable: TableView<UISearchResult> = TableView()

        val filename: TableColumn<UISearchResult, String> = TableColumn("File")
        val size: TableColumn<UISearchResult, String> = TableColumn("Size")
        val created: TableColumn<UISearchResult, String> = TableColumn("Created")
        val lastModified: TableColumn<UISearchResult, String> = TableColumn("Modified")

        filename.cellValueFactory = SimpleCellValueFactory { p -> p.filename }
        size.cellValueFactory = SimpleCellValueFactory { p -> p.size.toString() }
        created.cellValueFactory = SimpleCellValueFactory { p -> p.created.toString() }
        lastModified.cellValueFactory = SimpleCellValueFactory { p -> p.lastModified.toString() }

        resultTable.columns.addAll(filename, size, created, lastModified)
        resultTable.itemsProperty().bind(SimpleListProperty(searchManager.searchResults))

        resultTable.isFocusTraversable = false
        val hbox = HBox(10.0, resultTable)
        hbox.padding = Insets(10.0)
        hbox.alignment = Pos.CENTER
        HBox.setHgrow(resultTable, Priority.ALWAYS)
        VBox.setVgrow(hbox, Priority.ALWAYS)
        return hbox
    }

    private fun bottomControls(): Pane {
        val indexLabel = Label("Index")
        val createIndexButton = Button("Create")
        val leftBox = HBox(10.0, indexLabel, indexDropdown(), createIndexButton)
        HBox.setHgrow(leftBox, Priority.NEVER)
        leftBox.alignment = Pos.CENTER_LEFT

        val closeButton = Button("Close")
        val rightHBox = HBox(10.0, closeButton)
        HBox.setHgrow(rightHBox, Priority.ALWAYS)
        rightHBox.alignment = Pos.CENTER_RIGHT

        val hbox = HBox(10.0, leftBox, rightHBox)
        hbox.alignment = Pos.BASELINE_RIGHT
        hbox.padding = Insets(0.0, 10.0, 10.0, 10.0)

        return hbox
    }

    private fun indexDropdown(): ChoiceBox<IndexDropdownEntry> {
        val indexDropdown = ChoiceBox<IndexDropdownEntry>()
        val none = IndexDropdownEntry("-")
        val indexes = listOf(none, IndexDropdownEntry("C:\\index"), IndexDropdownEntry("C:\\index2"))
        indexDropdown.items.addAll(indexes)
        indexDropdown.value = none
        indexDropdown.converter = object : StringConverter<IndexDropdownEntry>() {
            override fun toString(entry: IndexDropdownEntry) = entry.indexPath

            override fun fromString(indexPath: String) = IndexDropdownEntry(indexPath)
        }
        return indexDropdown
    }
}

private class SimpleCellValueFactory(val converter: (UISearchResult) -> String) :
    Callback<TableColumn.CellDataFeatures<UISearchResult, String>, ObservableValue<String>> {

    override fun call(param: TableColumn.CellDataFeatures<UISearchResult, String>) = ReadOnlyObjectWrapper(converter.invoke(param.value))
}