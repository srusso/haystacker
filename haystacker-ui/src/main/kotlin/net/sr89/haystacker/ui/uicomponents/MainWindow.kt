package net.sr89.haystacker.ui.uicomponents

import javafx.beans.property.SimpleListProperty
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.ChoiceBox
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.Stage
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
        val resultList = ListView<String>()
        resultList.itemsProperty().bind(SimpleListProperty(searchManager.searchResults))
        resultList.isFocusTraversable = false
        val hbox = HBox(10.0, resultList)
        hbox.padding = Insets(10.0)
        hbox.alignment = Pos.CENTER
        HBox.setHgrow(resultList, Priority.ALWAYS)
        VBox.setVgrow(hbox,  Priority.ALWAYS)
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
//        val factory: Callback<ListView<IndexDropdownEntry>, ListCell<IndexDropdownEntry>> =
//            Callback<ListView<IndexDropdownEntry>, ListCell<IndexDropdownEntry>> { lv ->
//                MyListCell()
//            }
//        indexDropdown.cellFactory = factory
//        indexDropdown.buttonCell = factory.call(ListView(FXCollections.observableArrayList()))
        return indexDropdown
    }
}

class MyListCell: ListCell<IndexDropdownEntry>() {
    override fun updateItem(item: IndexDropdownEntry, empty: Boolean) {
        super.updateItem(item, empty)
        text = if (empty) {
            ""
        } else {
            item.indexPath
        }
    }
}