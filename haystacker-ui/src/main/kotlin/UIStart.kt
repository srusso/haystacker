package net.sr89.haystacker.ui

import javafx.application.Application
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.scene.text.Text
import javafx.stage.Stage


class UIStart: Application() {
    private fun createContent(): Parent {
        return StackPane(Text("Hello World"))
    }

    override fun start(stage: Stage) {
        stage.scene = Scene(createContent(), 300.0, 300.0)
        stage.show()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(UIStart::class.java)
        }
    }

}