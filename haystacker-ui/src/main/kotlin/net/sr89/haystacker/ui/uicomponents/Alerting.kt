package net.sr89.haystacker.ui.uicomponents

import javafx.application.Platform
import javafx.geometry.Insets
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.Tooltip
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.stage.Popup
import net.sr89.haystacker.ui.mainStage
import java.time.Duration
import java.util.concurrent.Executors

fun showAlert(title: String, content: String) {
    val alert = Alert(AlertType.INFORMATION)
    alert.title = title
    alert.contentText = content

    alert.showAndWait()
}

fun showMainWindowToolti2p(message: String, duration: Duration) {
    val tooltip = Tooltip(message)

    tooltip.showDuration = javafx.util.Duration.millis(duration.toMillis().toDouble())
    tooltip.show(mainStage)
}

fun showMainWindowTooltip(message: String, duration: Duration) {
    val tooltip = Popup()
    val text = Text(message)

    text.font = Font.font ("Verdana", 15.0)
    text.fill = Color.BLACK

    val box = VBox(10.0, text)

    box.background = Background(BackgroundFill(Color.GREY, CornerRadii(10.0), Insets(10.0)))

    tooltip.content.setAll(box)
    tooltip.show(mainStage)

    Executors.newSingleThreadExecutor().submit {
        Thread.sleep(duration.toMillis())
        Platform.runLater { tooltip.hide() }
    }
}