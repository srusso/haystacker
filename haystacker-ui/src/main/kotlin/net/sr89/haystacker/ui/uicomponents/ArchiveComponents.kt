package net.sr89.haystacker.ui.uicomponents

import javafx.geometry.Insets
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.paint.Color
import java.io.File

val selectedModeColor = Color(Color.LIGHTSKYBLUE.red, Color.LIGHTSKYBLUE.green, Color.LIGHTSKYBLUE.blue, 0.7)
val selectedModeColor2 = Color(Color.LIGHTSKYBLUE.red, Color.LIGHTSKYBLUE.green, Color.LIGHTSKYBLUE.blue, 0.8)
val selectedModeColor3 = Color(Color.LIGHTSKYBLUE.red, Color.LIGHTSKYBLUE.green, Color.LIGHTSKYBLUE.blue, 0.9)
val unselectedColor = Color(0.0, 0.0, 0.0, 0.0)

val selectedModeBackground = Background(
    BackgroundFill(selectedModeColor3, CornerRadii(10.0), Insets(25.0)),
    BackgroundFill(selectedModeColor2, CornerRadii(10.0), Insets(10.0)),
    BackgroundFill(selectedModeColor, CornerRadii(10.0), null)
)

val unselectedModeBackground = Background(
    BackgroundFill(unselectedColor, CornerRadii(10.0), null)
)

enum class ArchiveTargetType { DRIVE, DIRECTORY }

data class ArchiveTarget(
    val driveToArchive: String?,
    val dirToArchive: File?,
    val type: ArchiveTargetType
)