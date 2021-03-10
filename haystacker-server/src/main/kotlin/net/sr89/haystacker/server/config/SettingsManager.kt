package net.sr89.haystacker.server.config

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import net.sr89.haystacker.lang.exception.SettingsUpdateException
import net.sr89.haystacker.server.file.writeToFile
import java.io.IOException
import java.nio.file.Files.readAllBytes
import java.nio.file.Path

private data class HaystackerSettings(@JsonProperty("indexes") val indexes: Set<String>) {
    fun removeIndex(index: String): HaystackerSettings = HaystackerSettings(indexes.minus(index))
    fun addIndex(index: String): HaystackerSettings = HaystackerSettings(indexes.plus(index))
}

class SettingsManager(config: ServerConfig) {
    private val settingsFile: Path = config.settingsDirectory.resolve("haystacker-settings.json")
    private val objectMapper = ObjectMapper()

    private var settings = if (settingsFile.toFile().exists()) {
        objectMapper.readValue(readAllBytes(settingsFile), HaystackerSettings::class.java)!!
    } else {
        HaystackerSettings(emptySet())
    }

    fun indexes(): Set<String> {
        return settings.indexes
    }

    @Throws(SettingsUpdateException::class)
    fun addIndex(indexPath: String) =
        tryUpdateSettings(settings.addIndex(indexPath))

    @Throws(SettingsUpdateException::class)
    fun removeIndex(indexPath: String) =
        tryUpdateSettings(settings.removeIndex(indexPath))

    @Throws(SettingsUpdateException::class)
    private fun tryUpdateSettings(newSettings: HaystackerSettings) {
        try {
            writeToFile(objectMapper.writeValueAsString(newSettings), settingsFile.toFile())
            settings = newSettings
        } catch (e: IOException) {
            throw SettingsUpdateException(e.message!!)
        }
    }
}