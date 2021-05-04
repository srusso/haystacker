package net.sr89.haystacker.ui

import javafx.application.Application
import javafx.stage.Stage
import net.sr89.haystacker.ui.app.uiApplicationModule
import net.sr89.haystacker.ui.uicomponents.MainWindow
import org.kodein.di.instance
import org.kodein.di.newInstance

lateinit var stageBuilder: MainWindow

class UIStart: Application() {
    override fun start(stage: Stage) {
        return stageBuilder.buildStage(stage)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val di = uiApplicationModule()
            val appInstance by di.newInstance {
                MainWindow(searchManager = instance())
            }

            stageBuilder = appInstance

            launch(UIStart::class.java)
        }
    }
}