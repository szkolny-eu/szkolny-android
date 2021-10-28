/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-27.
 */

package pl.szczodrzynski.edziennik.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.config.db.ConfigEntry
import pl.szczodrzynski.edziennik.config.utils.ProfileConfigMigration
import pl.szczodrzynski.edziennik.config.utils.get
import pl.szczodrzynski.edziennik.config.utils.set
import pl.szczodrzynski.edziennik.config.utils.toHashMap
import pl.szczodrzynski.edziennik.data.db.AppDb
import kotlin.coroutines.CoroutineContext

class ProfileConfig(val db: AppDb, val profileId: Int, rawEntries: List<ConfigEntry>) : CoroutineScope, AbstractConfig {
    companion object {
        const val DATA_VERSION = 3
    }

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default

    val values: HashMap<String, String?> = hashMapOf()

    val grades by lazy { ProfileConfigGrades(this) }
    val ui by lazy { ProfileConfigUI(this) }
    val sync by lazy { ProfileConfigSync(this) }
    val attendance by lazy { ProfileConfigAttendance(this) }
    /*
    val timetable by lazy { ConfigTimetable(this) }
    val grades by lazy { ConfigGrades(this) }*/

    private var mDataVersion: Int? = null
    var dataVersion: Int
        get() { mDataVersion = mDataVersion ?: values.get("dataVersion", 0); return mDataVersion ?: 0 }
        set(value) { set("dataVersion", value); mDataVersion = value }

    private var mHash: String? = null
    var hash: String
        get() { mHash = mHash ?: values.get("hash", ""); return mHash ?: "" }
        set(value) { set("hash", value); mHash = value }

    init {
        rawEntries.toHashMap(profileId, values)
        if (dataVersion < DATA_VERSION)
            ProfileConfigMigration(this)
    }

    override fun set(key: String, value: String?) {
        values[key] = value
        launch {
            db.configDao().add(ConfigEntry(profileId, key, value))
        }
    }
}
