package net.sr89.haystacker.server.handlers

class HaystackerRoutes(
    val searchHandler: SearchHandler,
    val createIndexHandler: CreateIndexHandler,
    val directoryIndexHandler: DirectoryIndexHandler,
    val deindexHandler: DirectoryDeindexHandler,
    val taskProcessHandler: GetBackgroundTaskProgressHandler
)