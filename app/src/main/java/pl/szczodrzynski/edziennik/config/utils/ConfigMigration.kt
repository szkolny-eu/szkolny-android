/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-27.
 */

package pl.szczodrzynski.edziennik.config.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.BuildConfig
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.api.v2.LOGIN_TYPE_LIBRUS
import pl.szczodrzynski.edziennik.api.v2.LOGIN_TYPE_MOBIDZIENNIK
import pl.szczodrzynski.edziennik.api.v2.LOGIN_TYPE_VULCAN
import pl.szczodrzynski.edziennik.config.Config
import pl.szczodrzynski.edziennik.utils.models.Time

class ConfigMigration(app: App, config: Config) {
    init { config.apply {
        val p = app.getSharedPreferences("pl.szczodrzynski.edziennik_profiles", Context.MODE_PRIVATE)
        val s = "app.appConfig"

        if (dataVersion < 1) {
            ui.theme = p.getString("$s.appTheme", null)?.toIntOrNull() ?: 1
            sync.enabled = p.getString("$s.registerSyncEnabled", null)?.toBoolean() ?: true
            sync.interval = p.getString("$s.registerSyncEnabled", null)?.toIntOrNull() ?: 3600
            val oldButtons = p.getString("$s.miniDrawerButtonIds", null)?.let { str ->
                str.replace("[\\[\\]]*".toRegex(), "")
                        .split(",\\s?".toRegex())
                        .mapNotNull { it.toIntOrNull() }
            }
            ui.miniMenuButtons = oldButtons ?: listOf(
                    MainActivity.DRAWER_ITEM_HOME,
                    MainActivity.DRAWER_ITEM_TIMETABLE,
                    MainActivity.DRAWER_ITEM_AGENDA,
                    MainActivity.DRAWER_ITEM_GRADES,
                    MainActivity.DRAWER_ITEM_MESSAGES,
                    MainActivity.DRAWER_ITEM_HOMEWORK,
                    MainActivity.DRAWER_ITEM_SETTINGS
            )
            dataVersion = 1
        }
        if (dataVersion < 2) {
            devModePassword = p.getString("$s.devModePassword", null).fix()
            sync.tokenApp = p.getString("$s.fcmToken", null).fix()
            timetable.bellSyncMultiplier = p.getString("$s.bellSyncMultiplier", null)?.toIntOrNull() ?: 0
            sync.quietHoursStart = p.getString("$s.quietHoursStart", null)?.toLongOrNull() ?: 0
            appRateSnackbarTime = p.getString("$s.appRateSnackbarTime", null)?.toLongOrNull() ?: 0
            sync.quietHoursEnd = p.getString("$s.quietHoursEnd", null)?.toLongOrNull() ?: 0
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

            sync.tokenMobidziennikList = listOf()
            sync.tokenVulcanList = listOf()
            sync.tokenLibrusList = listOf()
            val tokens = p.getString("$s.fcmTokens", null)?.let { Gson().fromJson<Map<Int, Pair<String, List<Int>>>>(it, object: TypeToken<Map<Int, Pair<String, List<Int>>>>(){}.type) }
            tokens?.forEach {
                val token = it.value.first
                when (it.key) {
                    LOGIN_TYPE_MOBIDZIENNIK -> sync.tokenMobidziennik = token
                    LOGIN_TYPE_VULCAN -> sync.tokenVulcan = token
                    LOGIN_TYPE_LIBRUS -> sync.tokenLibrus = token
                }
            }
            dataVersion = 2
        }
    }}

    private fun String?.fix(): String? {
        return this?.replace("\"", "")?.let { if (it == "null") null else it }
    }
}