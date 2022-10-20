/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-19.
 */

package pl.szczodrzynski.edziennik.config.utils

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import pl.szczodrzynski.edziennik.BuildConfig
import pl.szczodrzynski.edziennik.config.Config
import pl.szczodrzynski.edziennik.data.db.enums.LoginType
import pl.szczodrzynski.edziennik.ext.asNavTargetOrNull
import pl.szczodrzynski.edziennik.ui.base.enums.NavTarget
import pl.szczodrzynski.edziennik.utils.models.Time
import kotlin.math.abs

class AppConfigMigrationV3(p: SharedPreferences, config: Config) {
    init { config.apply {
        val s = "app.appConfig"
        if (dataVersion < 1) {
            ui.theme = p.getString("$s.appTheme", null)?.toIntOrNull() ?: 1
            sync.enabled = p.getString("$s.registerSyncEnabled", null)?.toBoolean() ?: true
            sync.interval = p.getString("$s.registerSyncInterval", null)?.toIntOrNull() ?: 3600
            val oldButtons = p.getString("$s.miniDrawerButtonIds", null)?.let { str ->
                str.replace("[\\[\\]]*".toRegex(), "")
                        .split(",\\s?".toRegex())
                        .mapNotNull { it.toIntOrNull().asNavTargetOrNull() }
                        .toSet()
            }
            ui.miniMenuButtons = oldButtons ?: setOf(
                    NavTarget.HOME,
                    NavTarget.TIMETABLE,
                    NavTarget.AGENDA,
                    NavTarget.GRADES,
                    NavTarget.MESSAGES,
                    NavTarget.HOMEWORK,
                    NavTarget.SETTINGS
            )
            dataVersion = 1
        }
        if (dataVersion < 2) {
            devModePassword = p.getString("$s.devModePassword", null).fix()
            sync.tokenApp = p.getString("$s.fcmToken", null).fix()
            timetable.bellSyncMultiplier = p.getString("$s.bellSyncMultiplier", null)?.toIntOrNull() ?: 0
            appRateSnackbarTime = p.getString("$s.appRateSnackbarTime", null)?.toLongOrNull() ?: 0
            timetable.countInSeconds = p.getString("$s.countInSeconds", null)?.toBoolean() ?: false
            ui.headerBackground = p.getString("$s.headerBackground", null).fix()
            ui.appBackground = p.getString("$s.appBackground", null).fix()
            ui.language = p.getString("$s.language", null).fix()
            appVersion = p.getString("$s.lastAppVersion", null)?.toIntOrNull() ?: BuildConfig.VERSION_CODE
            appInstalledTime = p.getString("$s.appInstalledTime", null)?.toLongOrNull() ?: 0
            grades.orderBy = p.getString("$s.gradesOrderBy", null)?.toIntOrNull() ?: 0
            sync.quietDuringLessons = p.getString("$s.quietDuringLessons", null)?.toBoolean() ?: false
            ui.miniMenuVisible = p.getString("$s.miniDrawerVisible", null)?.toBoolean() ?: false
            loginFinished = p.getString("$s.loginFinished", null)?.toBoolean() ?: false
            sync.onlyWifi = p.getString("$s.registerSyncOnlyWifi", null)?.toBoolean() ?: false
            sync.notifyAboutUpdates = p.getString("$s.notifyAboutUpdates", null)?.toBoolean() ?: true
            timetable.bellSyncDiff = p.getString("$s.bellSyncDiff", null)?.let { Gson().fromJson(it, Time::class.java) }

            val startMillis = p.getString("$s.quietHoursStart", null)?.toLongOrNull() ?: 0
            val endMillis = p.getString("$s.quietHoursEnd", null)?.toLongOrNull() ?: 0
            if (startMillis > 0) {
                try {
                    sync.quietHoursStart = Time.fromMillis(abs(startMillis))
                    sync.quietHoursEnd = Time.fromMillis(abs(endMillis))
                    sync.quietHoursEnabled = true
                }
                catch (_: Exception) {}
            }
            else {
                sync.quietHoursEnabled = false
                sync.quietHoursStart = null
                sync.quietHoursEnd = null
            }

            sync.tokenMobidziennikList = listOf()
            sync.tokenVulcanList = listOf()
            sync.tokenLibrusList = listOf()
            val tokens = p.getString("$s.fcmTokens", null)?.let { Gson().fromJson<Map<Int, Pair<String, List<Int>>>>(it, object: TypeToken<Map<Int, Pair<String, List<Int>>>>(){}.type) }
            tokens?.forEach {
                val token = it.value.first
                when (it.key) {
                    LoginType.MOBIDZIENNIK.id -> sync.tokenMobidziennik = token
                    LoginType.VULCAN.id -> sync.tokenVulcan = token
                    LoginType.LIBRUS.id -> sync.tokenLibrus = token
                }
            }
            dataVersion = 2
        }
    }}

    private fun String?.fix(): String? {
        return this?.replace("\"", "")?.let { if (it == "null") null else it }
    }
}
