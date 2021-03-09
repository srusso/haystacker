package net.sr89.haystacker.server

import net.sr89.haystacker.async.task.AsyncBackgroundTaskManager
import net.sr89.haystacker.index.IndexManagerProvider
import net.sr89.haystacker.server.config.SettingsManager
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.factory
import org.kodein.di.instance
import org.kodein.di.singleton
import java.nio.file.Path

fun serverModule() = DI {
    bind<AsyncBackgroundTaskManager>() with singleton { AsyncBackgroundTaskManager() }
    bind<SettingsManager>() with factory { settingsDirectory: Path ->  SettingsManager(settingsDirectory) }
    bind<IndexManagerProvider>() with singleton { IndexManagerProvider(instance()) }
}