package net.sr89.haystacker.ui

import javafx.application.Application
import javafx.beans.binding.Bindings
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Stage


class UIStart: Application() {
    override fun start(stage: Stage) {
        val startLabel = Label("Search")
        val searchTestBox = TextField()
        searchTestBox.textProperty().bind(
            Bindings.format("HSL query here")
        )

        val gp = GridPane()
        gp.add(startLabel, 0, 0)
        gp.add(searchTestBox, 1, 0)
        gp.hgap = 10.0
        gp.vgap = 10.0

        val hbox = HBox(gp)
        hbox.alignment = Pos.TOP_CENTER

        val vbox = VBox(hbox)
        vbox.alignment = Pos.TOP_CENTER

        val scene = Scene(vbox, 480.0, 320.0)

        stage.scene = scene

        stage.title = "Haystacker"
        stage.show()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(UIStart::class.java)
        }
    }

}