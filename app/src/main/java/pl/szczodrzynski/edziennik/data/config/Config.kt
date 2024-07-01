/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-26.
 */

package pl.szczodrzynski.edziennik.data.config

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.BuildConfig
import pl.szczodrzynski.edziennik.data.config.migration.ConfigMigration11
import pl.szczodrzynski.edziennik.data.api.szkolny.response.RegisterAvailabilityStatus
import pl.szczodrzynski.edziennik.data.api.szkolny.response.Update
import pl.szczodrzynski.edziennik.data.config.migration.ConfigMigration14
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import pl.szczodrzynski.edziennik.data.enums.Theme
import pl.szczodrzynski.edziennik.ext.HOUR
import pl.szczodrzynski.edziennik.core.manager.GradesManager.Companion.ORDER_BY_DATE_DESC
import pl.szczodrzynski.edziennik.utils.models.Time

class Config(app: App) : BaseConfig<Config>(app, profileId = null) {

    override val dataVersion = 14
    override val migrations
        get() = mapOf(
            11 to ConfigMigration11(),
            14 to ConfigMigration14(),
        )

    private val profileConfigs: HashMap<Int, ProfileConfig> = hashMapOf()

    operator fun get(profileId: Int): ProfileConfig {
        var config = profileConfigs[profileId]
        if (config == null) {
            config = ProfileConfig(app, profileId, entries)
            profileConfigs[profileId] = config
        }
        config.migrate()
        return config
    }

    val ui by lazy { UI() }
    val sync by lazy { Sync() }
    val timetable by lazy { Timetable() }
    val grades by lazy { Grades() }

    var lastProfileId by config<Int>(0)
    var loginFinished by config<Boolean>(false)
    var privacyPolicyAccepted by config<Boolean>(false)
    var update by config<Update?>(null)
    var updatesChannel by config<String>("release")

    var devMode by config<Boolean?>("debugMode", null)
    var devModePassword by config<String?>(null)
    var enableChucker by config<Boolean?>(null)

    var apiAvailabilityCheck by config<Boolean>(true)
    var apiInvalidCert by config<String?>(null)
    var apiKeyCustom by config<String?>(null)
    var appInstalledTime by config<Long>(0L)
    var appRateSnackbarTime by config<Long>(0L)
    var lastLogCleanupTime by config<Long>(0L)
    var appVersion by config<Int>(BuildConfig.VERSION_CODE)
    var validation by config<String?>(null, "buildValidation")

    var archiverEnabled by config<Boolean>(true)
    var runSync by config<Boolean>(false)
    var widgetConfigs by config<JsonObject> { JsonObject() }

    inner class UI {
        var themeColor by config<Theme>(Theme.DEFAULT)
        var themeType by config<Theme.Type>(Theme.Type.M3)
        var themeMode by config<Theme.Mode>(Theme.Mode.DAYNIGHT)
        var themeNightMode by config<Boolean?>(null)
        var themeBlackMode by config<Boolean>(false)

        var language by config<String?>(null)

        var appBackground by config<String?>("appBg", null)
        var headerBackground by config<String?>("headerBg", null)

        var miniMenuVisible by config<Boolean>(false)
        var miniMenuButtons by config(NavTarget.Companion::getDefaultConfig)
        var openDrawerOnBackPressed by config<Boolean>(false)

        var bottomSheetOpened by config<Boolean>(false)
        var snowfall by config<Boolean>(false)
        var eggfall by config<Boolean>(false)
    }

    inner class Sync {
        var enabled by config<Boolean>("syncEnabled", true)
        var interval by config<Int>("syncInterval", 1 * HOUR.toInt())
        var onlyWifi by config<Boolean>("syncOnlyWifi", false)

        var dontShowAppManagerDialog by config<Boolean>(false)
        var lastAppSync by config<Long>(0L)
        var notifyAboutUpdates by config<Boolean>(true)
        var webPushEnabled by config<Boolean>(true)

        // Quiet Hours
        var quietHoursEnabled by config<Boolean>(false)
        var quietHoursStart by config<Time?>(null)
        var quietHoursEnd by config<Time?>(null)
        var quietDuringLessons by config<Boolean>(false)

        // FCM Tokens
        var tokenApp by config<String?>(null)
        var tokenMobidziennik by config<String?>(null)
        var tokenLibrus by config<String?>(null)
        var tokenVulcan by config<String?>(null)
        var tokenVulcanHebe by config<String?>(null)

        var tokenMobidziennikList by config<List<Int>> { listOf() }
        var tokenLibrusList by config<List<Int>> { listOf() }
        var tokenVulcanList by config<List<Int>> { listOf() }
        var tokenVulcanHebeList by config<List<Int>> { listOf() }

        // Register Availability
        private var registerAvailabilityMap by config<Map<String, RegisterAvailabilityStatus>>("registerAvailability") { mapOf() }
        private var registerAvailabilityFlavor by config<String?>(null)

        var registerAvailability: Map<String, RegisterAvailabilityStatus>
            get() {
                if (BuildConfig.FLAVOR != registerAvailabilityFlavor)
                    return mapOf()
                return registerAvailabilityMap
            }
            set(value) {
                registerAvailabilityMap = value
                registerAvailabilityFlavor = BuildConfig.FLAVOR
            }
    }

    inner class Timetable {
        var bellSyncMultiplier by config<Int>(0)
        var bellSyncDiff by config<Time?>(null)
        var countInSeconds by config<Boolean>(false)
    }

    inner class Grades {
        var orderBy by config<Int>("gradesOrderBy", ORDER_BY_DATE_DESC)
    }
}
