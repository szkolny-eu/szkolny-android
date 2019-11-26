/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-26.
 */

package pl.szczodrzynski.edziennik.config

import pl.szczodrzynski.edziennik.MainActivity

class ConfigMigration(config: Config) {
    init { config.apply {
        if (dataVersion < 1) {
            ui.theme = 1
            sync.enabled = true
            sync.interval = 60*60; // seconds
            ui.miniMenuButtons = listOf(
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
    }}
}