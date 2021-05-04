package net.sr89.haystacker.ui

import javafx.application.Application
import javafx.beans.property.SimpleListProperty
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import net.sr89.haystacker.ui.search.SearchManager


class UIStart: Application() {

    private val searchManager = SearchManager()

    override fun start(stage: Stage) {
        val vbox = VBox(searchBoxPanel(), resultsListView())
        vbox.alignment = Pos.TOP_CENTER

        val scene = Scene(vbox, 480.0, 320.0)

        stage.scene = scene

        stage.title = "Haystacker"
        stage.show()
    }

    private fun searchBoxPanel(): Pane {
        val searchLabel = Label("Search")
        val searchTestBox = TextField()
        searchTestBox.promptText = "type file name.."
        searchTestBox.isFocusTraversable = false
        searchTestBox.onKeyTyped = EventHandler {
            searchManager.onSimpleSearchUpdate(searchTestBox.text)
        }

        val hbox = HBox(10.0, searchLabel, searchTestBox)
        hbox.alignment = Pos.CENTER

        return hbox
    }

    private fun resultsListView(): Pane {
        val resultList = ListView<String>()
        resultList.itemsProperty().bind(SimpleListProperty(searchManager.actualResults))

        val hbox = HBox(10.0, resultList)
        hbox.alignment = Pos.CENTER

        return hbox
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(UIStart::class.java)
        }
    }

}