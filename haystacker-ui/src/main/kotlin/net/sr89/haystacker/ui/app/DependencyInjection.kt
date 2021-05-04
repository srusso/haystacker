package net.sr89.haystacker.ui.app

import net.sr89.haystacker.server.api.HaystackerRestClient
import net.sr89.haystacker.ui.UIStageBuilder
import net.sr89.haystacker.ui.search.SearchManager
import org.http4k.client.ApacheClient
import org.http4k.core.HttpHandler
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton

fun managerModule() = DI.Module("Managers") {
    bind<SearchManager>() with singleton { SearchManager(instance()) }
    bind<HttpHandler>(tag = "apacheClient") with singleton { ApacheClient() }
    bind<HaystackerRestClient>() with singleton { HaystackerRestClient("http://localhost:9000", instance(tag = "apacheClient")) }
}

fun uiApplicationModule() = DI {
    import(managerModule())

    bind<UIStageBuilder>() with singleton { UIStageBuilder(instance()) }
}