package net.sr89.haystacker.server.handlers

import net.sr89.haystacker.index.IndexManagerProvider
import net.sr89.haystacker.server.api.directory
import net.sr89.haystacker.server.api.indexPath
import net.sr89.haystacker.server.api.stringBody
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import java.nio.file.Paths

class DirectoryDeindexHandler(val indexManagerProvider: IndexManagerProvider): HttpHandler {
    override fun invoke(request: Request): Response {
        val indexPath: String = indexPath(request)
        val directoryToDeindex = Paths.get(directory(request))

        println("Received request to remove directory $directoryToDeindex from index $indexPath")

        return if (!directoryToDeindex.toFile().exists()) {
            Response(Status.NOT_FOUND).with(stringBody of "Directory $directoryToDeindex not found")
        } else if (!Paths.get(indexPath).toFile().exists()) {
            Response(Status.NOT_FOUND).with(stringBody of "Index at $indexPath not found")
        } else {
            val indexManager = indexManagerProvider.forPath(indexPath)

            indexManager.removeDirectoryFromIndex(directoryToDeindex, true)

            Response(Status.OK)
        }
    }
}