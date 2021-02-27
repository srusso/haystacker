package net.sr89.haystacker.server.config

import java.nio.file.Path

class HaystackerSettings {
    private val settingsFileName = "haystacker-settings.json"
    private var settingsDirectory: String = "./"

    fun setSettingsDirectory(file: String) {
        settingsDirectory = file
    }

    fun getSettingsDirectory() = settingsDirectory

    fun indexes(): List<Path> {
        return listOf()
    }

    fun addIndex(indexPath: Path) {

    }
}