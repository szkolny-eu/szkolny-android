/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-26.
 */

package pl.szczodrzynski.edziennik.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.data.db.AppDb
import kotlin.coroutines.CoroutineContext

class Config(val db: AppDb) : CoroutineScope {
    companion object {
        const val DATA_VERSION = 1
    }

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default

    private var profileId: Int? = null
    val values: HashMap<String, String?> = hashMapOf()
    //private val profileValues: HashMap<String, String?> = hashMapOf()

    val ui by lazy { ConfigUI(this) }
    val sync by lazy { ConfigSync(this) }
    val timetable by lazy { ConfigTimetable(this) }
    val grades by lazy { ConfigGrades(this) }

    private var mDataVersion: Int? = null
    var dataVersion: Int
        get() { mDataVersion = mDataVersion ?: values.get("dataVersion", 0); return mDataVersion ?: 0 }
        set(value) { set(-1, "dataVersion", value); mDataVersion = value }

    init {
        db.configDao().getAllNow().toHashMap(values)
        if (dataVersion < DATA_VERSION)
            ConfigMigration(this)
    }
    fun setProfile(profileId: Int) {
        this.profileId = profileId
        //profileValues.clear()
        //db.configDao().getAllNow(profileId).toHashMap(profileValues)
    }

    fun set(profileId: Int, key: String, value: String?) {
        //if (profileId == -1)
        values[key] = value
        /*else
            profileValues[key] = value*/
        launch {
            db.configDao().add(ConfigEntry(profileId, key, value))
        }
    }
}