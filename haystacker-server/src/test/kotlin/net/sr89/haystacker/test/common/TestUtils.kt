package net.sr89.haystacker.test.common

import net.sr89.haystacker.server.api.SearchResponse
import net.sr89.haystacker.server.api.SearchResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.FileTime
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals

fun Path.setTimes(lastModified: Instant, created: Instant): Path {
    Files.getFileAttributeView(this, BasicFileAttributeView::class.java)
        .setTimes(FileTime.from(lastModified), null, FileTime.from(created))
    return this
}

fun assertSearchResult(searchResponse: SearchResponse, expectedFilenames: List<String>, message: String? = null) {
    val foundFilenames: List<String> = searchResponse.results
        .map(SearchResult::path)
        .map { path -> Paths.get(path).fileName.toString() }

    assertEquals(expectedFilenames.toSet(), foundFilenames.toSet(), message)
}

fun assertSortedSearchResult(
    searchResult: SearchResponse,
    expectedFilenamesInOrder: List<String>,
    message: String? = null
) {
    val foundFilenames: List<String> = searchResult.results
        .map(SearchResult::path)
        .map { path -> Paths.get(path).fileName.toString() }

    assertEquals(expectedFilenamesInOrder, foundFilenames, message)
}

fun createServerTestFiles(
    fileTime: Instant,
    directoryToIndex: Path,
    subDirectory: Path
) {
    val oldFile = directoryToIndex.resolve("oldFile.txt")

    Files.newOutputStream(oldFile).use {
        it.write("Some example file contents".toByteArray())
    }
    oldFile.setTimes(fileTime, fileTime)

    Files.newOutputStream(directoryToIndex.resolve("binary.dat")).use {
        it.write(ByteArray(10) { i -> i.toByte() })
    }

    Files.newOutputStream(directoryToIndex.resolve("bigbinary.dat")).use {
        it.write(ByteArray(1000) { i -> i.toByte() })
    }

    Files.createDirectory(subDirectory)

    Files.newOutputStream(subDirectory.resolve("subFile.txt")).use {
        it.write("Some example file contents (subdirectory file)".toByteArray())
    }
}

fun tryAssertingRepeatedly(timeout: Duration, action: () -> Unit) {
    val start = System.nanoTime()

    lateinit var assertionError: AssertionError

    while (durationSince(start) < timeout) {
        try {
            action()
            return
        } catch (e: AssertionError) {
            assertionError = e
            Thread.sleep(5L)
        }
    }

    throw assertionError
}

fun durationSince(nanos: Long): Duration = Duration.ofNanos(System.nanoTime() - nanos)