package net.sr89.haystacker.server

import net.sr89.haystacker.async.task.AsyncBackgroundTaskManager
import net.sr89.haystacker.async.task.BackgroundTaskManager
import net.sr89.haystacker.index.IndexManagerProvider
import net.sr89.haystacker.server.config.SettingsManager
import net.sr89.haystacker.server.handlers.CreateIndexHandler
import net.sr89.haystacker.server.handlers.DirectoryDeindexHandler
import net.sr89.haystacker.server.handlers.DirectoryIndexHandler
import net.sr89.haystacker.server.handlers.GetBackgroundTaskProgressHandler
import net.sr89.haystacker.server.handlers.HaystackerRoutes
import net.sr89.haystacker.server.handlers.SearchHandler
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.factory
import org.kodein.di.instance
import org.kodein.di.singleton
import java.nio.file.Path

val handlersModule = DI.Module(name = "HandlersModule") {
    bind<SearchHandler>() with singleton { SearchHandler(instance()) }
    bind<CreateIndexHandler>() with factory { settingsDirectory: Path -> CreateIndexHandler(instance(), instance(arg = settingsDirectory)) }
    bind<DirectoryIndexHandler>() with singleton { DirectoryIndexHandler(instance(), instance()) }
    bind<DirectoryDeindexHandler>() with singleton { DirectoryDeindexHandler(instance()) }
    bind<GetBackgroundTaskProgressHandler>() with singleton { GetBackgroundTaskProgressHandler(instance()) }

    bind<HaystackerRoutes>() with factory { settingsDirectory: Path -> HaystackerRoutes(
        instance(),
        instance(arg = settingsDirectory),
        instance(),
        instance(),
        instance()
    ) }
}

val managerModule = DI.Module("Managers") {
    bind<BackgroundTaskManager>() with singleton { AsyncBackgroundTaskManager() }
    bind<SettingsManager>() with factory { settingsDirectory: Path ->  SettingsManager(settingsDirectory) }
    bind<IndexManagerProvider>() with singleton { IndexManagerProvider(instance()) }
}

fun serverDI() = DI {
    import(handlersModule)
    import(managerModule)
}