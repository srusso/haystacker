package net.sr89.haystacker.ui.uicomponents

import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.Border
import javafx.scene.layout.BorderStroke
import javafx.scene.layout.BorderStrokeStyle
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.stage.Popup
import net.sr89.haystacker.server.calc.boundValue
import net.sr89.haystacker.ui.mainStage
import java.time.Duration
import java.util.concurrent.Executors

fun showAlert(title: String, content: String) {
    val alert = Alert(AlertType.INFORMATION)
    alert.title = title
    alert.contentText = content

    alert.showAndWait()
}

fun showMainWindowTooltip(message: String, duration: Duration) {
    fun textPaint(opacity: Double) = Color(Color.BLACK.red, Color.BLACK.green, Color.BLACK.blue, opacity)
    fun backgroundPaint(opacity: Double) =
        Background(
            BackgroundFill(
                Color(Color.GREY.red, Color.GREY.green, Color.GREY.blue, opacity),
                null,
                null
            )
        )

    fun border(opacity: Double) =
        Border(
            BorderStroke(
                Color(Color.BLACK.red, Color.BLACK.green, Color.BLACK.blue, opacity),
                BorderStrokeStyle.SOLID,
                null,
                null
            )
        )

    val tooltip = Popup()
    val text = Text(message)
    val box = VBox(15.0, text)

    text.font = Font.font("Verdana", 15.0)
    text.fill = textPaint(1.0)
    box.background = backgroundPaint(1.0)
    box.border = border(1.0)

    tooltip.content.setAll(box)

    Executors.newSingleThreadExecutor().submit {
        Platform.runLater {
            tooltip.show(
                mainStage,
                mainStage.x + mainStage.width / 2 - tooltip.width / 2,
                mainStage.y + mainStage.height / 2 - tooltip.height / 2
            )
        }
        val durationNanos = duration.toNanos()
        val start = System.nanoTime()
        val end = start + durationNanos

        while (true) {
            Thread.sleep(10L)
            val currentNanos = System.nanoTime()
            val opacity: Double = (1.0 - ((currentNanos - start) / durationNanos.toDouble())).boundValue(0.0, 1.0)
            Platform.runLater {
                text.fill = textPaint(opacity)
                box.background = backgroundPaint(opacity)
                box.border = border(opacity)
            }
            if (System.nanoTime() >= end) {
                break
            }
        }

        Platform.runLater { tooltip.hide() }
    }
}