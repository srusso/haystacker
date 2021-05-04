package net.sr89.haystacker.ui

import javafx.application.Application
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleListProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.stage.Stage


class UIStart: Application() {

    val actualResults = FXCollections.observableArrayList<String>()

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
        searchTestBox.textProperty().bind(
            Bindings.format("HSL query here")
        )

        val hbox = HBox(10.0, searchLabel, searchTestBox)
        hbox.alignment = Pos.CENTER

        return hbox
    }

    private fun resultsListView(): Pane {
        val resultList = ListView<String>()
        resultList.itemsProperty().bind(SimpleListProperty(actualResults))

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