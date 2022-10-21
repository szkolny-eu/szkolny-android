/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-27.
 */

package pl.szczodrzynski.edziennik.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.config.db.ConfigEntry
import pl.szczodrzynski.edziennik.data.db.AppDb
import pl.szczodrzynski.edziennik.ext.takePositive
import kotlin.coroutines.CoroutineContext

abstract class BaseConfig(
    val db: AppDb,
    val profileId: Int? = null,
    protected var entries: List<ConfigEntry>? = null,
) : CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default

    val values = hashMapOf<String, String?>()

    init {
        if (entries == null)
            entries = db.configDao().getAllNow()
        values.clear()
        for ((profileId, key, value) in entries!!) {
            if (profileId.takePositive() != this.profileId)
                continue
            values[key] = value
        }
    }

    fun set(key: String, value: String?) {
        values[key] = value
        launch(Dispatchers.IO) {
            db.configDao().add(ConfigEntry(profileId ?: -1, key, value))
        }
    }
}
