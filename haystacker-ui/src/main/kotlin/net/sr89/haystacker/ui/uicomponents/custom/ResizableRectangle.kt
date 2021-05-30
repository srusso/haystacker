package net.sr89.haystacker.ui.uicomponents.custom

import javafx.scene.shape.Rectangle

class ResizableRectangle(width: Double, height: Double): Rectangle(width, height) {
    override fun isResizable() = true

    override fun minWidth(height: Double) = width

    override fun minHeight(width: Double) = height
}