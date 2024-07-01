/*
 * Copyright (c) Kuba Szczodrzyński 2024-7-1.
 */

package pl.szczodrzynski.edziennik.core.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.db.entity.LogEntry
import pl.szczodrzynski.edziennik.ext.DAY
import pl.szczodrzynski.edziennik.ext.MS
import pl.szczodrzynski.edziennik.ext.ignore
import timber.log.Timber

class LoggingManager(val app: App) : CoroutineScope {
    companion object {
        private const val CLEANUP_INTERVAL = 2 * DAY * MS
        private const val CLEANUP_MAX_AGE = 7 * DAY * MS
    }

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

    suspend fun cleanupIfNeeded(force: Boolean = false) {
        if (!force && System.currentTimeMillis() - app.config.lastLogCleanupTime < CLEANUP_INTERVAL)
            return
        withContext(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis() - CLEANUP_MAX_AGE
            Timber.i("Cleaning logs older than $timestamp")
            app.db.logDao().clearBefore(timestamp)
        }
        app.config.lastLogCleanupTime = System.currentTimeMillis()
    }

    fun cleanupHyperLogDatabase() {
        try {
            app.deleteDatabase("com.hypertrack.common.device_logs.db")
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
}
