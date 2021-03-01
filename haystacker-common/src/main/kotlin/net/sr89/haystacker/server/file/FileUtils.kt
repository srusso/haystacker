package net.sr89.haystacker.server.file

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files

@Throws(IOException::class)
fun writeToFile(contents: String, destination: File) {
    val temporaryFile = File(destination.name + "." + System.currentTimeMillis())

    FileOutputStream(temporaryFile).use {
        it.write(contents.toByteArray())
    }

    if (destination.exists()) destination.delete()

    if (destination.exists()) {
        throw IOException("Could not update destination file ${destination.name}")
    }

    Files.move(temporaryFile.toPath(), destination.toPath())
}