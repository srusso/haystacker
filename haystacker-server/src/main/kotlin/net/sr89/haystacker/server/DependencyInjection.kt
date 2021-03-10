package net.sr89.haystacker.server

import net.sr89.haystacker.async.task.AsyncBackgroundTaskManager
import net.sr89.haystacker.async.task.BackgroundTaskManager
import net.sr89.haystacker.index.IndexManagerProvider
import net.sr89.haystacker.server.config.ServerConfig
import net.sr89.haystacker.server.config.SettingsManager
import net.sr89.haystacker.server.filter.ExceptionHandlingFilter
import net.sr89.haystacker.server.handlers.CreateIndexHandler
import net.sr89.haystacker.server.handlers.DirectoryDeindexHandler
import net.sr89.haystacker.server.handlers.DirectoryIndexHandler
import net.sr89.haystacker.server.handlers.GetBackgroundTaskProgressHandler
import net.sr89.haystacker.server.handlers.HaystackerRoutes
import net.sr89.haystacker.server.handlers.QuitHandler
import net.sr89.haystacker.server.handlers.SearchHandler
import org.http4k.core.RequestContexts
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.multiton
import org.kodein.di.singleton
import java.time.Duration

private fun buildRestServer(haystackerRoutes: HaystackerRoutes, port: Int): Http4kServer {
    val contexts = RequestContexts()

    val app = ServerFilters.InitialiseRequestContext(contexts)
        .then(ExceptionHandlingFilter())
        .then(ServerFilters.CatchLensFailure())
        .then(haystackerRoutes.routesHandler())

    return app.asServer(Jetty(port))
}

val handlersModule = DI.Module(name = "HandlersModule") {
    bind<SearchHandler>() with singleton { SearchHandler(instance()) }
    bind<CreateIndexHandler>() with multiton { conf: ServerConfig -> CreateIndexHandler(instance(), instance(arg = conf)) }
    bind<DirectoryIndexHandler>() with singleton { DirectoryIndexHandler(instance(), instance()) }
    bind<DirectoryDeindexHandler>() with singleton { DirectoryDeindexHandler(instance()) }
    bind<GetBackgroundTaskProgressHandler>() with singleton { GetBackgroundTaskProgressHandler(instance()) }
    bind<QuitHandler>() with singleton { QuitHandler(instance(), instance(), instance(tag = "shutdownDelay")) }

    bind<HaystackerRoutes>() with multiton { conf: ServerConfig ->
        HaystackerRoutes(
            instance(),
            instance(arg = conf),
            instance(),
            instance(),
            instance(),
            instance()
        )
    }
}

val managerModule = DI.Module("Managers") {
    bind<BackgroundTaskManager>() with singleton { AsyncBackgroundTaskManager() }
    bind<SettingsManager>() with multiton { conf: ServerConfig -> SettingsManager(conf) }
    bind<IndexManagerProvider>() with singleton { IndexManagerProvider(instance()) }
    bind<Http4kServer>() with multiton { conf: ServerConfig -> buildRestServer(instance(arg = conf), conf.httpPort) }
    bind<Duration>(tag = "shutdownDelay") with singleton { Duration.ofSeconds(5) }
}

fun applicationModule() = DI {
    import(handlersModule)
    import(managerModule)
}