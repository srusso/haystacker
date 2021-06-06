package net.sr89.haystacker.ui.uicomponents

import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType

fun showAlert(title: String, content: String) {
    val alert = Alert(AlertType.INFORMATION)
    alert.title = title
    alert.contentText = content

    alert.showAndWait()
}