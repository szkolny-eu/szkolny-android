/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-27.
 */

package pl.szczodrzynski.edziennik.data.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.db.entity.ConfigEntry
import pl.szczodrzynski.edziennik.ext.takePositive
import kotlin.coroutines.CoroutineContext

abstract class BaseConfig<T>(
    @Transient
    val app: App,
    val profileId: Int? = null,
    protected var entries: List<ConfigEntry>? = null,
) : CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default

    // public for lab
    val values = hashMapOf<String, String?>()

    private var currentDataVersion: Int by config<Int>("dataVersion") {
        currentDataVersion = dataVersion
        dataVersion
    }
    var hash by config<String>("")

    abstract val dataVersion: Int
    abstract val migrations: Map<Int, BaseMigration<T>>

    init {
        if (entries == null)
            entries = app.db.configDao().getAllNow()
        values.clear()
        for ((profileId, key, value) in entries!!) {
            if (profileId.takePositive() != this.profileId)
                continue
            values[key] = value
        }
    }

    fun migrate() {
        if (this.dataVersion == this.currentDataVersion)
            return
        var dataVersion = this.currentDataVersion
        while (dataVersion < this.dataVersion) {
            @Suppress("UNCHECKED_CAST")
            migrations[++dataVersion]?.let {
                it.db = app.db
                it.profileId = profileId
                it.migrate(this as T)
            }
        }
        this.currentDataVersion = dataVersion
        this.hash = ""
    }

    operator fun set(key: String, value: String?) {
        values[key] = value
        launch(Dispatchers.IO) {
            app.db.configDao().add(ConfigEntry(profileId ?: -1, key, value))
        }
    }

    operator fun get(key: String) = values[key]

    operator fun contains(key: String) = key in values
}
