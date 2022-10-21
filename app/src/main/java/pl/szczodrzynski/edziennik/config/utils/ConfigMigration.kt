/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-27.
 */

package pl.szczodrzynski.edziennik.config.utils

import android.content.Context
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.BuildConfig
import pl.szczodrzynski.edziennik.config.Config
import pl.szczodrzynski.edziennik.ext.HOUR
import pl.szczodrzynski.edziennik.ui.base.enums.NavTarget
import pl.szczodrzynski.edziennik.utils.managers.GradesManager.Companion.ORDER_BY_DATE_DESC
import pl.szczodrzynski.edziennik.utils.models.Time
import kotlin.math.abs

class ConfigMigration(app: App, config: Config) {
    init { config.apply {

        val p = app.getSharedPreferences("pl.szczodrzynski.edziennik_profiles", Context.MODE_PRIVATE)
        if (p.contains("app.appConfig.appTheme")) {
            // migrate appConfig from app version 3.x and lower.
            // Updates dataVersion to level 2.
            AppConfigMigrationV3(p, config)
        }

        if (dataVersion < 2) {
            appVersion = BuildConfig.VERSION_CODE
            loginFinished = false
            ui.language = null
            ui.theme = 1
            ui.appBackground = null
            ui.headerBackground = null
            ui.miniMenuVisible = false
            ui.miniMenuButtons = setOf(
                    NavTarget.HOME,
                    NavTarget.TIMETABLE,
                    NavTarget.AGENDA,
                    NavTarget.GRADES,
                    NavTarget.MESSAGES,
                    NavTarget.HOMEWORK,
                    NavTarget.SETTINGS
            )
            sync.enabled = true
            sync.interval = 1* HOUR.toInt()
            sync.notifyAboutUpdates = true
            sync.onlyWifi = false
            sync.quietHoursEnabled = false
            sync.quietHoursStart = null
            sync.quietHoursEnd = null
            sync.quietDuringLessons = false
            sync.tokenApp = null
            sync.tokenMobidziennik = null
            sync.tokenMobidziennikList = listOf()
            sync.tokenLibrus = null
            sync.tokenLibrusList = listOf()
            sync.tokenVulcan = null
            sync.tokenVulcanList = listOf()
            timetable.bellSyncMultiplier = 0
            timetable.bellSyncDiff = null
            timetable.countInSeconds = false
            grades.orderBy = ORDER_BY_DATE_DESC

            dataVersion = 2
        }

        if (dataVersion < 3) {
            update = null
            privacyPolicyAccepted = false
            devMode = null
            devModePassword = null
            appInstalledTime = 0L
            appRateSnackbarTime = 0L

            dataVersion = 3
        }

        if (dataVersion < 10) {
            ui.openDrawerOnBackPressed = false
            ui.snowfall = false
            ui.bottomSheetOpened = false
            sync.dontShowAppManagerDialog = false
            sync.webPushEnabled = true
            sync.lastAppSync = 0L


            dataVersion = 10
        }

        if (dataVersion < 11) {
            val startMillis = config.values["quietHoursStart"]?.toLongOrNull() ?: 0L
            val endMillis = config.values["quietHoursEnd"]?.toLongOrNull() ?: 0L
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

            dataVersion = 11
        }
    }}
}
