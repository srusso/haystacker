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
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.paint.Color.LIGHTSEAGREEN
import javafx.scene.shape.Rectangle
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
    private val unselectedColor = Color(0.0, 0.0, 0.0, 0.0)

    val driveRectangle = Rectangle(100.0, 100.0)
    val folderRectangle = Rectangle(100.0, 100.0)

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

        driveRectangle.fill = unselectedColor
        folderRectangle.fill = unselectedColor

        val driveModeSelected = EventHandler<MouseEvent> {
            Platform.runLater {
                driveRectangle.fill = selectedModeColor
                folderRectangle.fill = unselectedColor
            }
            archiveTarget = DRIVE
        }

        val onFolderToArchiveSelection = EventHandler<MouseEvent> {
            val fileChooser = DirectoryChooser()
            fileChooser.title = "Folder to archive"
            dirToArchive = fileChooser.showDialog(stage)

            if (dirToArchive != null) {
                Platform.runLater {
                    driveRectangle.fill = unselectedColor
                    folderRectangle.fill = selectedModeColor
                    driveDropdown.value = none
                    archiveFolderLabel.text = "Archive the \"${dirToArchive!!.name}\" directory"
                }

                archiveTarget = DIRECTORY
            }
        }

        driveSelection.onMousePressed = driveModeSelected
        driveDropdown.onMouseClicked = driveModeSelected

        folderSelection.onMousePressed = EventHandler {
            if (dirToArchive != null) {
                driveRectangle.fill = unselectedColor
                folderRectangle.fill = selectedModeColor
            } else {
                onFolderToArchiveSelection.handle(it)
            }
        }
        selectFolderButton.onMouseClicked = onFolderToArchiveSelection

        val hbox = HBox(10.0, driveSelection, folderSelection)
        hbox.padding = Insets(0.0, 10.0, 10.0, 10.0)

        return hbox
    }

    private fun driveSelection(driveDropdown: ChoiceBox<String>): Pane {
        driveRectangle.fill = unselectedColor
        val driveSelectionBox = VBox(10.0, driveRectangle, archiveDriveLabel, driveDropdown)
        HBox.setHgrow(driveSelectionBox, Priority.ALWAYS)
        driveRectangle.arcHeight = 10.0
        driveRectangle.arcWidth = 10.0
        driveSelectionBox.alignment = Pos.CENTER
        driveSelectionBox.background = Background(BackgroundFill(unselectedColor, null, null))
        val stackPane = StackPane(driveRectangle, driveSelectionBox)
        return stackPane
    }

    private fun folderSelection(selectFolderButton: Button): Pane {
        folderRectangle.fill = unselectedColor

        val folderSelectionBox = VBox(10.0, archiveFolderLabel, selectFolderButton)
        HBox.setHgrow(folderSelectionBox, Priority.ALWAYS)
        folderSelectionBox.alignment = Pos.CENTER

        folderRectangle.arcHeight = 10.0
        folderRectangle.arcWidth = 10.0

        folderSelectionBox.background = Background(BackgroundFill(unselectedColor, null, null))

        val stackPane = StackPane(folderRectangle, folderSelectionBox)
        return stackPane
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