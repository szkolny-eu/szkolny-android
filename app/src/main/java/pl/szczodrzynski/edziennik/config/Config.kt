/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-26.
 */

package pl.szczodrzynski.edziennik.config

import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.BuildConfig
import pl.szczodrzynski.edziennik.config.db.ConfigEntry
import pl.szczodrzynski.edziennik.config.utils.ConfigMigration
import pl.szczodrzynski.edziennik.config.utils.get
import pl.szczodrzynski.edziennik.config.utils.set
import pl.szczodrzynski.edziennik.config.utils.toHashMap
import pl.szczodrzynski.edziennik.data.api.szkolny.response.Update
import pl.szczodrzynski.edziennik.data.db.AppDb
import kotlin.coroutines.CoroutineContext

class Config(val db: AppDb) : CoroutineScope, AbstractConfig {
    companion object {
        const val DATA_VERSION = 10
    }

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default

    val values: HashMap<String, String?> = hashMapOf()

    val ui by lazy { ConfigUI(this) }
    val sync by lazy { ConfigSync(this) }
    val timetable by lazy { ConfigTimetable(this) }
    val grades by lazy { ConfigGrades(this) }

    private var mDataVersion: Int? = null
    var dataVersion: Int
        get() { mDataVersion = mDataVersion ?: values.get("dataVersion", 0); return mDataVersion ?: 0 }
        set(value) { set("dataVersion", value); mDataVersion = value }

    private var mHash: String? = null
    var hash: String
        get() { mHash = mHash ?: values.get("hash", ""); return mHash ?: "" }
        set(value) { set("hash", value); mHash = value }

    private var mLastProfileId: Int? = null
    var lastProfileId: Int
        get() { mLastProfileId = mLastProfileId ?: values.get("lastProfileId", 0); return mLastProfileId ?: 0 }
        set(value) { set("lastProfileId", value); mLastProfileId = value }

    private var mUpdatesChannel: String? = null
    var updatesChannel: String
        get() { mUpdatesChannel = mUpdatesChannel ?: values.get("updatesChannel", "release"); return mUpdatesChannel ?: "release" }
        set(value) { set("updatesChannel", value); mUpdatesChannel = value }
    private var mUpdate: Update? = null
    var update: Update?
        get() { mUpdate = mUpdate ?: values.get("update", null as Update?); return mUpdate ?: null as Update? }
        set(value) { set("update", value); mUpdate = value }

    private var mAppVersion: Int? = null
    var appVersion: Int
        get() { mAppVersion = mAppVersion ?: values.get("appVersion", BuildConfig.VERSION_CODE); return mAppVersion ?: BuildConfig.VERSION_CODE }
        set(value) { set("appVersion", value); mAppVersion = value }

    private var mLoginFinished: Boolean? = null
    var loginFinished: Boolean
        get() { mLoginFinished = mLoginFinished ?: values.get("loginFinished", false); return mLoginFinished ?: false }
        set(value) { set("loginFinished", value); mLoginFinished = value }

    private var mPrivacyPolicyAccepted: Boolean? = null
    var privacyPolicyAccepted: Boolean
        get() { mPrivacyPolicyAccepted = mPrivacyPolicyAccepted ?: values.get("privacyPolicyAccepted", false); return mPrivacyPolicyAccepted ?: false }
        set(value) { set("privacyPolicyAccepted", value); mPrivacyPolicyAccepted = value }

    private var mDevModePassword: String? = null
    var devModePassword: String?
        get() { mDevModePassword = mDevModePassword ?: values.get("devModePassword", null as String?); return mDevModePassword }
        set(value) { set("devModePassword", value); mDevModePassword = value }

    private var mAppInstalledTime: Long? = null
    var appInstalledTime: Long
        get() { mAppInstalledTime = mAppInstalledTime ?: values.get("appInstalledTime", 0L); return mAppInstalledTime ?: 0L }
        set(value) { set("appInstalledTime", value); mAppInstalledTime = value }

    private var mAppRateSnackbarTime: Long? = null
    var appRateSnackbarTime: Long
        get() { mAppRateSnackbarTime = mAppRateSnackbarTime ?: values.get("appRateSnackbarTime", 0L); return mAppRateSnackbarTime ?: 0L }
        set(value) { set("appRateSnackbarTime", value); mAppRateSnackbarTime = value }

    private var mRunSync: Boolean? = null
    var runSync: Boolean
        get() { mRunSync = mRunSync ?: values.get("runSync", false); return mRunSync ?: false }
        set(value) { set("runSync", value); mRunSync = value }

    private var mWidgetConfigs: JsonObject? = null
    var widgetConfigs: JsonObject
        get() { mWidgetConfigs = mWidgetConfigs ?: values.get("widgetConfigs", JsonObject()); return mWidgetConfigs ?: JsonObject() }
        set(value) { set("widgetConfigs", value); mWidgetConfigs = value }

    private var mLastAppSync: Long? = null
    var lastAppSync: Long
        get() { mLastAppSync = mLastAppSync ?: values.get("lastAppSync", 0L); return mLastAppSync ?: 0L }
        set(value) { set("lastAppSync", value); mLastAppSync = value }

    private var rawEntries: List<ConfigEntry> = db.configDao().getAllNow()
    private val profileConfigs: HashMap<Int, ProfileConfig> = hashMapOf()
    init {
        rawEntries.toHashMap(-1, values)
    }
    fun migrate(app: App) {
        if (dataVersion < DATA_VERSION)
            ConfigMigration(app, this)
    }
    fun getFor(profileId: Int): ProfileConfig {
        return profileConfigs[profileId] ?: ProfileConfig(db, profileId, db.configDao().getAllNow(profileId)).also {
            profileConfigs[profileId] = it
        }
    }
    fun forProfile() = getFor(App.profileId)

    fun setProfile(profileId: Int) {
    }

    override fun set(key: String, value: String?) {
        values[key] = value
        launch {
            db.configDao().add(ConfigEntry(-1, key, value))
        }
    }
}
