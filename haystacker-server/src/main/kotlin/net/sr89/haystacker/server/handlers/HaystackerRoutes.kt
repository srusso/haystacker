package net.sr89.haystacker.server.handlers

data class HaystackerRoutes(
    val searchHandler: SearchHandler,
    val createIndexHandler: CreateIndexHandler,
    val directoryIndexHandler: DirectoryIndexHandler,
    val deindexHandler: DirectoryDeindexHandler,
    val taskProcessHandler: GetBackgroundTaskProgressHandler
)