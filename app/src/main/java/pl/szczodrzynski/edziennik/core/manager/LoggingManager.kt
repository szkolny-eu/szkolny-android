/*
 * Copyright (c) Kuba Szczodrzyński 2024-7-1.
 */

package pl.szczodrzynski.edziennik.core.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.db.entity.LogEntry
import pl.szczodrzynski.edziennik.ext.ignore
import timber.log.Timber

class LoggingManager(val app: App) : CoroutineScope {

    override val coroutineContext = Job() + Dispatchers.IO

    val logcatTree = object : Timber.DebugTree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            super.log(
                priority = priority,
                tag = tag?.substringBefore("$")?.let { "Szkolny/$it" },
                message = message,
                t = t,
            )
        }
    }

    val databaseTree = object : Timber.DebugTree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) = launch {
            val entry = LogEntry(
                timestamp = System.currentTimeMillis(),
                priority = priority,
                tag = tag?.substringBefore("$"),
                message = message,
            )
            app.db.logDao().add(entry)
        }.ignore()
    }
}
