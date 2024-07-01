/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-27.
 */

package pl.szczodrzynski.edziennik.data.config

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.db.entity.ConfigEntry
import pl.szczodrzynski.edziennik.ext.takePositive
import pl.szczodrzynski.edziennik.utils.Utils.d
import kotlin.coroutines.CoroutineContext

abstract class BaseConfig<T>(
    @Transient
    val app: App,
    val profileId: Int? = null,
    protected var entries: List<ConfigEntry>? = null,
) : CoroutineScope {
    companion object {
        private const val TAG = "BaseConfig"
    }

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default

    // public for lab
    val values = hashMapOf<String, String?>()

    private var currentDataVersion: Int by config<Int>("dataVersion") {
        Log.d(TAG, "Initializing ${this::class.java.simpleName} version $dataVersion")
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
            if (profileId != (this.profileId ?: -1))
                continue
            values[key] = value
            Log.d(
                TAG,
                "Loaded ${this::class.java.simpleName} profile $profileId key $key value $value"
            )
        }
    }

    fun migrate() {
        if (this.dataVersion == this.currentDataVersion) {
            Log.d(
                TAG,
                "Config ${this::class.java.simpleName} is up to date (${this.currentDataVersion})"
            )
            return
        }
        Log.d(
            TAG,
            "Migrating ${this::class.java.simpleName} ${this.currentDataVersion} -> ${this.dataVersion}"
        )
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
            d(TAG, "Setting config value ($profileId): $key = $value")
            app.db.configDao().add(ConfigEntry(profileId ?: -1, key, value))
        }
    }

    operator fun get(key: String) = values[key]

    operator fun contains(key: String) = key in values
}
