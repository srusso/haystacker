package net.sr89.haystacker.ui.uicomponents

import javafx.application.Platform
import javafx.beans.value.ObservableValue
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
import javafx.scene.layout.Border
import javafx.scene.layout.BorderStroke
import javafx.scene.layout.BorderStrokeStyle.SOLID
import javafx.scene.layout.BorderWidths
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.paint.Color.LIGHTSKYBLUE
import javafx.scene.text.TextAlignment
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

    private val selectedModeColor = Color(LIGHTSKYBLUE.red, LIGHTSKYBLUE.green, LIGHTSKYBLUE.blue, 0.7)
    private val unselectedColor = Color(0.0, 0.0, 0.0, 0.0)

    private val selectedModeBackground = Background(
        BackgroundFill(selectedModeColor, CornerRadii(10.0), null)
    )

    private val unselectedModeBackground = Background(
        BackgroundFill(unselectedColor, CornerRadii(10.0), null)
    )

    private val none = "-"
    private val driveList: ObservableList<String> = FXCollections.observableArrayList(none)

    // TODO remove these from here, create them in show() and pass them around as necessary
    private var dirToArchive: File? = null
    private var archiveTarget: ArchiveTarget? = null

    fun show() {
        dirToArchive = null
        archiveTarget = null

        archiveDriveLabel.isWrapText = true
        archiveFolderLabel.isWrapText = true

        val stage = Stage()

        val vbox = VBox(10.0, archiveModeChoice(stage), bottomControls(stage))
        vbox.alignment = Pos.CENTER
        vbox.autosize()

        val scene = Scene(vbox, 480.0, 320.0)

        stage.scene = scene

        archiveDriveLabel.border = Border(BorderStroke(Color.BLACK, SOLID, CornerRadii(10.0), BorderWidths(1.0)))
        archiveFolderLabel.border = Border(BorderStroke(Color.BLACK, SOLID, CornerRadii(10.0), BorderWidths(1.0)))

        archiveFolderLabel.maxWidth = 150.0
        archiveDriveLabel.maxWidth = 150.0
        archiveDriveLabel.textAlignment = TextAlignment.CENTER
        archiveFolderLabel.textAlignment = TextAlignment.CENTER

        stage.title = "Create Archive"
        stage.minWidth = 520.0
        stage.minHeight = 320.0
        stage.isResizable = false
        stage.show()

        archiveFolderLabel.minWidth = archiveFolderLabel.width
        archiveDriveLabel.minWidth = archiveFolderLabel.width
        archiveFolderLabel.alignment = Pos.CENTER
        archiveDriveLabel.alignment = Pos.CENTER
    }

    private fun archiveModeChoice(stage: Stage): Pane {
        val driveDropdown = driveDropdown()
        driveDropdown.selectionModel.selectedItemProperty().addListener { _: ObservableValue<out String>?, _: String?, selectedDrive: String? ->
            if (selectedDrive != null) {
                Platform.runLater {
                    archiveDriveLabel.text = "Archive the $selectedDrive drive"
                }
            }
        }
        driveDropdown.selectionModel.select(none)
        val driveSelection = driveSelection(driveDropdown)
        val selectFolderButton = Button("Select")
        val folderSelection = folderSelection(selectFolderButton)

        driveSelection.border = Border(BorderStroke(Color.BLACK, SOLID, CornerRadii(10.0), BorderWidths(1.0)))
        folderSelection.border = Border(BorderStroke(Color.BLACK, SOLID, CornerRadii(10.0), BorderWidths(1.0)))

        val driveModeSelected = EventHandler<MouseEvent> {
            Platform.runLater {
                driveSelection.background = selectedModeBackground
                folderSelection.background = unselectedModeBackground
            }
            archiveTarget = DRIVE
        }

        val onFolderToArchiveSelection = EventHandler<MouseEvent> {
            val fileChooser = DirectoryChooser()
            fileChooser.title = "Folder to archive"
            dirToArchive = fileChooser.showDialog(stage)

            if (dirToArchive != null) {
                Platform.runLater {
                    driveSelection.background = unselectedModeBackground
                    folderSelection.background = selectedModeBackground
                    archiveFolderLabel.text = "Archive the \"${dirToArchive!!.name}\" directory"
                    selectFolderButton.text = "Change"
                }

                archiveTarget = DIRECTORY
            }
        }

        driveSelection.onMousePressed = driveModeSelected
        driveDropdown.onMouseClicked = driveModeSelected

        folderSelection.onMousePressed = EventHandler {
            if (dirToArchive != null) {
                driveSelection.background = unselectedModeBackground
                folderSelection.background = selectedModeBackground
            } else {
                onFolderToArchiveSelection.handle(it)
            }
        }
        selectFolderButton.onMouseClicked = onFolderToArchiveSelection

        HBox.setHgrow(driveSelection, Priority.ALWAYS)
        HBox.setHgrow(folderSelection, Priority.ALWAYS)
        val hbox = HBox(10.0, driveSelection, folderSelection)
        hbox.alignment = Pos.CENTER
        hbox.padding = Insets(10.0, 10.0, 10.0, 10.0)
        hbox.border = Border(BorderStroke(Color.BLACK, SOLID, CornerRadii(10.0), BorderWidths(1.0)))
        VBox.setVgrow(hbox, Priority.ALWAYS)
        return hbox
    }

    private fun driveSelection(driveDropdown: ChoiceBox<String>): Pane {
        val driveSelectionBox = VBox(10.0, archiveDriveLabel, driveDropdown)
//        HBox.setHgrow(driveSelectionBox, Priority.NEVER)
        driveSelectionBox.alignment = Pos.CENTER
        driveSelectionBox.background = unselectedModeBackground
        return driveSelectionBox
    }

    private fun folderSelection(selectFolderButton: Button): Pane {
        val folderSelectionBox = VBox(10.0, archiveFolderLabel, selectFolderButton)
//        HBox.setHgrow(folderSelectionBox, Priority.NEVER)
        folderSelectionBox.alignment = Pos.CENTER
        folderSelectionBox.background = unselectedModeBackground
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