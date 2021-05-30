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
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.paint.Color.LIGHTSEAGREEN
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import net.sr89.haystacker.server.api.HaystackerRestClient
import net.sr89.haystacker.ui.uicomponents.CreateArchiveWizard.ArchiveTarget.DIRECTORY
import net.sr89.haystacker.ui.uicomponents.CreateArchiveWizard.ArchiveTarget.DRIVE
import java.io.File

class CreateArchiveWizard(private val restClient: HaystackerRestClient) {

    enum class ArchiveTarget { DRIVE, DIRECTORY }

    private val archiveDriveLabel = Label("Archive a whole drive")
    private val archiveFolderLabel = Label("Archive a single folder (more can be added later)")

    private val selectedModeColor = Color(LIGHTSEAGREEN.red, LIGHTSEAGREEN.green, LIGHTSEAGREEN.blue, 0.7)
    private val selectedModeBackground = Background(
        BackgroundFill(Color.BLACK, CornerRadii(8.0), null),
        BackgroundFill(selectedModeColor, CornerRadii(10.0), null)
    )

    private val unselectedModeBackground = Background(
        BackgroundFill(Color.BLACK, CornerRadii(8.0), null),
        BackgroundFill(Color.GRAY, CornerRadii(10.0), null)
    )

    private val none = "-"
    private val driveList: ObservableList<String> = FXCollections.observableArrayList(none)

    private var dirToArchive: File? = null

    private var archiveTarget: ArchiveTarget? = null

    fun show() {
        val stage = Stage()

        val vbox = VBox(10.0, archiveModeChoice(stage), bottomControls(stage))
        vbox.alignment = Pos.CENTER

        val scene = Scene(vbox, 480.0, 320.0)

        stage.scene = scene

        stage.title = "Create Archive"
        stage.show()

//        stage.onCloseRequest = EventHandler {  }
    }

    private fun archiveModeChoice(stage: Stage): Pane {
        val driveDropdown = driveDropdown()
        val driveSelection = driveSelection(driveDropdown)
        val selectFolderButton = Button("Select")

        val folderSelection = folderSelection(selectFolderButton)

        driveSelection.background = unselectedModeBackground
        folderSelection.background = selectedModeBackground

        val driveModeSelected = EventHandler<MouseEvent> {
            driveSelection.background = selectedModeBackground
            folderSelection.background = unselectedModeBackground
            archiveTarget = DRIVE
        }

        val onFolderToArchiveSelection = EventHandler<MouseEvent> {
            val fileChooser = DirectoryChooser()
            fileChooser.title = "Folder to archive"
            dirToArchive = fileChooser.showDialog(stage)

            if (dirToArchive != null) {
                driveSelection.background = unselectedModeBackground
                folderSelection.background = selectedModeBackground

                Platform.runLater {
                    driveDropdown.value = none
                }

                archiveTarget = DIRECTORY
                folderSelection.children.setAll(archiveFolderLabel, Label(dirToArchive!!.name), selectFolderButton)
            }
        }

        driveSelection.onMousePressed = driveModeSelected
        driveDropdown.onMouseClicked = driveModeSelected

        folderSelection.onMousePressed = EventHandler {
            driveSelection.background = unselectedModeBackground
            folderSelection.background = selectedModeBackground
            if (dirToArchive == null) {
                onFolderToArchiveSelection.handle(it)
            }
        }
        selectFolderButton.onMouseClicked = onFolderToArchiveSelection

        val hbox = HBox(10.0, driveSelection, folderSelection)
        hbox.padding = Insets(0.0, 10.0, 10.0, 10.0)

        return hbox
    }

    private fun driveSelection(driveDropdown: ChoiceBox<String>): Pane {
        val driveSelectionBox = VBox(10.0, archiveDriveLabel, driveDropdown)
        HBox.setHgrow(driveSelectionBox, Priority.ALWAYS)
        driveSelectionBox.alignment = Pos.CENTER
        return driveSelectionBox
    }

    private fun folderSelection(selectFolderButton: Button): Pane {
        val folderSelectionBox = VBox(10.0, archiveFolderLabel, selectFolderButton)
        HBox.setHgrow(folderSelectionBox, Priority.ALWAYS)
        folderSelectionBox.alignment = Pos.CENTER
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
        nextButton.onMouseClicked = EventHandler { }
        val rightHBox = HBox(10.0, nextButton)
        HBox.setHgrow(rightHBox, Priority.ALWAYS)
        rightHBox.alignment = Pos.CENTER_RIGHT

        val hbox = HBox(10.0, leftBox, rightHBox)
        hbox.alignment = Pos.BASELINE_RIGHT
        hbox.padding = Insets(0.0, 10.0, 10.0, 10.0)

        return hbox
    }
}