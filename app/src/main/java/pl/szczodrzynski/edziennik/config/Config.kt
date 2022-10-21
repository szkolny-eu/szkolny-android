/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-26.
 */

package pl.szczodrzynski.edziennik.config

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.BuildConfig
import pl.szczodrzynski.edziennik.config.utils.*
import pl.szczodrzynski.edziennik.data.api.szkolny.response.Update
import pl.szczodrzynski.edziennik.data.db.AppDb

@Suppress("RemoveExplicitTypeArguments")
class Config(db: AppDb) : BaseConfig(db) {
    companion object {
        const val DATA_VERSION = 12
    }

    private val profileConfigs: HashMap<Int, ProfileConfig> = hashMapOf()

    val ui by lazy { ConfigUI(this) }
    val sync by lazy { ConfigSync(this) }
    val timetable by lazy { ConfigTimetable(this) }
    val grades by lazy { ConfigGrades(this) }

    var dataVersion by config<Int>(0)
    var hash by config<String>("")

    var lastProfileId by config<Int>(0)
    var loginFinished by config<Boolean>(false)
    var privacyPolicyAccepted by config<Boolean>(false)
    var update by config<Update?>(null)
    var updatesChannel by config<String>("release")

    var devMode by config<Boolean?>(null)
    var devModePassword by config<String?>(null)
    var enableChucker by config<Boolean?>(null)

    var apiAvailabilityCheck by config<Boolean>(true)
    var apiInvalidCert by config<String?>(null)
    var appInstalledTime by config<Long>(0L)
    var appRateSnackbarTime by config<Long>(0L)
    var appVersion by config<Int>(BuildConfig.VERSION_CODE)
    var validation by config<String?>(null, "buildValidation")

    var archiverEnabled by config<Boolean>(true)
    var runSync by config<Boolean>(false)
    var widgetConfigs by config<JsonObject> { JsonObject() }

    fun migrate(app: App) {
        if (dataVersion < DATA_VERSION)
            ConfigMigration(app, this)
    }

    fun getFor(profileId: Int): ProfileConfig {
        return profileConfigs[profileId] ?: ProfileConfig(db, profileId, entries).also {
            profileConfigs[profileId] = it
        }
    }

    fun forProfile() = getFor(App.profileId)
}
