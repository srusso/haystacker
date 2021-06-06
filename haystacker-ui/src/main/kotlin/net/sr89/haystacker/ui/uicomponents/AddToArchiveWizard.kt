package net.sr89.haystacker.ui.uicomponents

import javafx.application.Platform
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ChoiceBox
import javafx.scene.control.Label
import javafx.scene.control.Separator
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.text.TextAlignment
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import net.sr89.haystacker.server.api.HaystackerRestClient
import org.http4k.core.Status
import java.io.File

class AddToArchiveWizard(private val restClient: HaystackerRestClient) {
    private val wizard = Wizard("Add to archive")

    fun show(archiveLocation: File) {
        wizard.start(AddToArchiveSelectTargetStep(archiveLocation, wizard, restClient))
    }
}

private class AddToArchiveSelectTargetStep(
    private val archiveLocation: File,
    private val wizard: Wizard,
    private val restClient: HaystackerRestClient
) : WizardStep {

    val addButton = Button("Add")

    private val archiveDriveLabel = Label("Archive a whole drive")
    private val archiveFolderLabel = Label("Archive a single folder (more can be added later)")

    private val none = "-"
    private val driveDropdown = ChoiceBox<String>()
    private val driveList: ObservableList<String> = FXCollections.observableArrayList(none)

    // TODO remove these from here, create them in show() and pass them around as necessary
    private var dirToArchive: File? = null
    private var archiveTarget: ArchiveTargetType = ArchiveTargetType.DRIVE

    override fun scene(stage: Stage) = buildScene(stage)

    override fun createNextStep(): WizardStep? = null

    private fun buildScene(stage: Stage): Scene {
        archiveDriveLabel.isWrapText = true
        archiveFolderLabel.isWrapText = true

        val vbox = VBox(10.0, archiveModeChoice(stage), bottomControls())
        vbox.alignment = Pos.CENTER
        vbox.autosize()

        val scene = Scene(vbox, 480.0, 320.0)

        archiveFolderLabel.maxWidth = 150.0
        archiveDriveLabel.maxWidth = 150.0
        archiveDriveLabel.textAlignment = TextAlignment.CENTER
        archiveFolderLabel.textAlignment = TextAlignment.CENTER

        archiveFolderLabel.minWidth = archiveFolderLabel.width
        archiveDriveLabel.minWidth = archiveFolderLabel.width
        archiveFolderLabel.alignment = Pos.CENTER
        archiveDriveLabel.alignment = Pos.CENTER
        return scene
    }

    private fun archiveModeChoice(stage: Stage): Pane {
        val driveDropdown = driveDropdown()
        driveDropdown.selectionModel.selectedItemProperty()
            .addListener { _: ObservableValue<out String>?, _: String?, selectedDrive: String? ->
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

        val driveModeSelected = EventHandler<MouseEvent> {
            Platform.runLater {
                driveSelection.background = selectedModeBackground
                folderSelection.background = unselectedModeBackground
            }
            archiveTarget = ArchiveTargetType.DRIVE
        }

        driveModeSelected.handle(null)

        val onFolderToArchiveSelection = EventHandler<MouseEvent> {
            val fileChooser = DirectoryChooser()
            fileChooser.title = "Folder to archive"
            dirToArchive = fileChooser.showDialog(stage)

            if (dirToArchive != null) {
                Platform.runLater {
                    addButton.isDisable = false
                    driveSelection.background = unselectedModeBackground
                    folderSelection.background = selectedModeBackground
                    val toArchive = dirToArchive!!.name.ifEmpty { dirToArchive!!.path }
                    archiveFolderLabel.text = "Archive the \"$toArchive\" directory"
                    selectFolderButton.text = "Change"
                }

                archiveTarget = ArchiveTargetType.DIRECTORY
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

        val separator = Separator()
        separator.orientation = Orientation.VERTICAL

        val hbox = HBox(10.0, driveSelection, separator, folderSelection)
        hbox.alignment = Pos.CENTER
        hbox.padding = Insets(10.0, 10.0, 10.0, 10.0)
        VBox.setVgrow(hbox, Priority.ALWAYS)
        return hbox
    }

    private fun driveSelection(driveDropdown: ChoiceBox<String>): Pane {
        val driveSelectionBox = VBox(10.0, archiveDriveLabel, driveDropdown)
        driveSelectionBox.alignment = Pos.CENTER
        driveSelectionBox.background = unselectedModeBackground
        return driveSelectionBox
    }

    private fun folderSelection(selectFolderButton: Button): Pane {
        val folderSelectionBox = VBox(10.0, archiveFolderLabel, selectFolderButton)
        folderSelectionBox.alignment = Pos.CENTER
        folderSelectionBox.background = unselectedModeBackground
        return folderSelectionBox
    }

    private fun driveDropdown(): ChoiceBox<String> {
        driveDropdown.items = driveList
        driveDropdown.value = none

        // todo implement "runUntilUpToTimes(task, condition, maxTimes) in BackgroundTaskManager
        // and reimplement this to re-try the call until successful
        val volumes = restClient.listVolumes().responseBody().volumes
        Platform.runLater {
            if (volumes.isNotEmpty()) {
                addButton.isDisable = false
                driveList.setAll(volumes)
                driveDropdown.value = volumes[0]
            }
        }

        return driveDropdown
    }

    private fun bottomControls(): Pane {
        val cancelButton = Button("Cancel")
        cancelButton.onMouseClicked = EventHandler { wizard.close() }

        val leftBox = HBox(10.0, cancelButton)
        HBox.setHgrow(leftBox, Priority.NEVER)
        leftBox.alignment = Pos.CENTER_LEFT

        addButton.isDisable = true
        addButton.onMouseClicked = EventHandler {
            if (addToArchive(ArchiveTarget(driveDropdown.value, dirToArchive, archiveTarget), archiveLocation) ) {
                showAlert("Archiving started", "The archive is being populated")
                wizard.close()
            } else {
                showAlert("ERROR!", "Could not populate the archive. Is the haystacker server running?")
            }
        }
        val rightHBox = HBox(10.0, addButton)
        HBox.setHgrow(rightHBox, Priority.ALWAYS)
        rightHBox.alignment = Pos.CENTER_RIGHT

        val hbox = HBox(10.0, leftBox, rightHBox)
        hbox.alignment = Pos.BASELINE_RIGHT
        hbox.padding = Insets(0.0, 10.0, 10.0, 10.0)

        return hbox
    }

    private fun addToArchive(archiveTarget: ArchiveTarget, archiveLocation: File): Boolean {
        val dirToIndex: String =
            if (archiveTarget.type == ArchiveTargetType.DIRECTORY) archiveTarget.dirToArchive!!.toString() else archiveTarget.driveToArchive!!

        val indexDirectoryResult = restClient.indexDirectory(archiveLocation.toString(), dirToIndex)

        return indexDirectoryResult.status == Status.OK
    }
}