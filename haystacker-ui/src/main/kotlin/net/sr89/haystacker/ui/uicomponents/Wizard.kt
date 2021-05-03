package net.sr89.haystacker.ui.uicomponents

import javafx.scene.Scene
import javafx.stage.Stage

interface WizardStep {
    fun scene(stage: Stage): Scene
    fun createNextStep(): WizardStep?
}

class Wizard(val title: String) {
    private lateinit var stage: Stage
    private lateinit var currentStep: WizardStep

    fun start(initialStep: WizardStep) {
        stage = Stage()
        currentStep = initialStep
        stage.scene = currentStep.scene(stage)
        stage.title = title
        stage.minWidth = 520.0
        stage.minHeight = 320.0
        stage.isResizable = false
        stage.show()
    }

    fun nextStage() {
        val nextStep = currentStep.createNextStep()

        if (nextStep != null) {
            currentStep = nextStep
            stage.scene = currentStep.scene(stage)
        } else {
            close()
        }
    }

    fun close() {
        stage.hide()
    }
}