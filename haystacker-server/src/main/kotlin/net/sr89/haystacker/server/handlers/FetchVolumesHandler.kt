package net.sr89.haystacker.server.handlers

import net.sr89.haystacker.filesystem.VolumeManager
import net.sr89.haystacker.server.api.ListVolumesResponse
import net.sr89.haystacker.server.api.listVolumesResponse
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with

class FetchVolumesHandler(val volumeManager: VolumeManager) : HttpHandler {
    override fun invoke(request: Request): Response {
        val indexes = ListVolumesResponse(volumeManager.currentlyDetectedVolumes().map { p -> p.toString() }.sorted())
        return Response(Status.OK).with(listVolumesResponse of indexes)
    }
}