package net.sr89.haystacker.ui.uicomponents

import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ChoiceBox
import javafx.scene.control.Label
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.paint.Color.LIGHTSEAGREEN
import javafx.stage.Stage
import net.sr89.haystacker.server.api.HaystackerRestClient

class CreateArchiveWizard(private val restClient: HaystackerRestClient) {

    private val selectedModeColor = Color(LIGHTSEAGREEN.red, LIGHTSEAGREEN.green, LIGHTSEAGREEN.blue, 0.7)
    private val selectedModeBackground = Background(BackgroundFill(selectedModeColor, CornerRadii(10.0), null))

    private val none = "-"
    private val driveList: ObservableList<String> = FXCollections.observableArrayList(none)

    fun show() {
        val stage = Stage()

        val vbox = VBox(10.0, selectionBox(), bottomControls(stage))
        vbox.alignment = Pos.CENTER

        val scene = Scene(vbox, 480.0, 320.0)

        stage.scene = scene

        stage.title = "Create Archive"
        stage.show()

//        stage.onCloseRequest = EventHandler {  }
    }

    private fun selectionBox(): Pane {
        val driveDropdown = driveDropdown()
        val driveSelection = driveSelection(driveDropdown)
        val folderSelection = folderSelection()

        driveSelection.onMousePressed = EventHandler {
            driveSelection.background = selectedModeBackground
            folderSelection.background = null
        }

        driveDropdown.onMouseClicked = EventHandler {
            driveSelection.background = selectedModeBackground
            folderSelection.background = null
        }

        folderSelection.onMousePressed = EventHandler {
            driveSelection.background = null
            folderSelection.background = selectedModeBackground
            Platform.runLater {
                driveDropdown.value = none
            }
        }

        val hbox = HBox(10.0, driveSelection, folderSelection)
//        hbox.alignment = Pos.BASELINE_RIGHT
        hbox.padding = Insets(0.0, 10.0, 10.0, 10.0)

        return hbox
    }

    private fun driveSelection(driveDropdown: ChoiceBox<String>): Pane {
        val driveSelectionBox = VBox(10.0, Label("Archive a whole drive"), driveDropdown)
        HBox.setHgrow(driveSelectionBox, Priority.ALWAYS)
        driveSelectionBox.alignment = Pos.CENTER_LEFT
        return driveSelectionBox
    }

    private fun folderSelection(): Pane {
        val folderSelectionBox = VBox(10.0, Label("Archive a single folder (more can be added later)"))
        HBox.setHgrow(folderSelectionBox, Priority.ALWAYS)
        return folderSelectionBox
    }

    private fun driveDropdown(): ChoiceBox<String> {
        val driveDropdown = ChoiceBox<String>()
        driveDropdown.items = driveList
        driveDropdown.value = none

        // todo implement "runUntilUpToTimes(task, condition, maxTimes) in BackgroundTaskManager
        // and reimplement this to re-try the call until successful
        val volumes = restClient.listVolumes().responseBody().volumes
        Platform.runLater {
            driveList.setAll(listOf(none).plus(volumes))
            driveDropdown.value = none
        }

        return driveDropdown
    }

    private fun bottomControls(stage: Stage): Pane {
        val cancelButton = Button("Cancel")
        cancelButton.onMouseClicked = EventHandler { stage.hide() }

        val leftBox = HBox(10.0, cancelButton)
        HBox.setHgrow(leftBox, Priority.NEVER)
        leftBox.alignment = Pos.CENTER_LEFT

        val nextButton = Button("Next")
        nextButton.onMouseClicked = EventHandler {  }
        val rightHBox = HBox(10.0, nextButton)
        HBox.setHgrow(rightHBox, Priority.ALWAYS)
        rightHBox.alignment = Pos.CENTER_RIGHT

        val hbox = HBox(10.0, leftBox, rightHBox)
        hbox.alignment = Pos.BASELINE_RIGHT
        hbox.padding = Insets(0.0, 10.0, 10.0, 10.0)

        return hbox
    }
}