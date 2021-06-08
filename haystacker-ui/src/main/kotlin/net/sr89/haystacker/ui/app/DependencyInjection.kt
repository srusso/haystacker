package net.sr89.haystacker.ui.app

import net.sr89.haystacker.server.api.HaystackerRestClient
import net.sr89.haystacker.server.async.task.AsyncBackgroundTaskManager
import net.sr89.haystacker.server.async.task.BackgroundTaskManager
import net.sr89.haystacker.ui.search.SearchManager
import net.sr89.haystacker.ui.uicomponents.AddToArchiveWizard
import net.sr89.haystacker.ui.uicomponents.IndexDropdownManager
import net.sr89.haystacker.ui.uicomponents.MainWindow
import net.sr89.haystacker.ui.uicomponents.ServerStatusComponent
import org.http4k.client.ApacheClient
import org.http4k.core.HttpHandler
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

val utilModule = DI.Module(name = "UtilsModule") {
    bind<ExecutorService>() with singleton { Executors.newCachedThreadPool() }
}

fun managerModule() = DI.Module("Managers") {
    bind<BackgroundTaskManager>() with singleton { AsyncBackgroundTaskManager(instance()) }
    bind<IndexDropdownManager>() with singleton { IndexDropdownManager(instance(), instance()) }
    bind<ServerStatusComponent>() with singleton { ServerStatusComponent(instance(), instance()) }
    bind<SearchManager>() with singleton { SearchManager(instance(), instance(), instance()) }
    bind<HttpHandler>(tag = "apacheClient") with singleton { ApacheClient() }
    bind<HaystackerRestClient>() with singleton { HaystackerRestClient("http://localhost:9000", instance(tag = "apacheClient")) }
}

fun uiApplicationModule() = DI {
    import(utilModule)
    import(managerModule())

    bind<AddToArchiveWizard>() with singleton { AddToArchiveWizard(instance()) }
    bind<MainWindow>() with singleton { MainWindow(
        instance(),
        instance(),
        instance(),
        instance()
    ) }
}