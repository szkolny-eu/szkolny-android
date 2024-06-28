/*
 * Copyright (c) Kuba Szczodrzyński 2024-6-28.
 */

package pl.szczodrzynski.edziennik.data.config.migration

import pl.szczodrzynski.edziennik.data.config.BaseMigration
import pl.szczodrzynski.edziennik.data.config.Config
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import pl.szczodrzynski.edziennik.data.enums.Theme.BLUE
import pl.szczodrzynski.edziennik.data.enums.Theme.DEFAULT
import pl.szczodrzynski.edziennik.data.enums.Theme.GREEN
import pl.szczodrzynski.edziennik.data.enums.Theme.Mode.CLASSIC
import pl.szczodrzynski.edziennik.data.enums.Theme.Mode.DAYNIGHT
import pl.szczodrzynski.edziennik.data.enums.Theme.Mode.FULL
import pl.szczodrzynski.edziennik.data.enums.Theme.PURPLE
import pl.szczodrzynski.edziennik.data.enums.Theme.RED
import pl.szczodrzynski.edziennik.data.enums.Theme.TEAL
import pl.szczodrzynski.edziennik.data.enums.Theme.Type.M3
import pl.szczodrzynski.edziennik.ext.toJsonArray

class ConfigMigration13 : BaseMigration<Config>() {

    override fun migrate(config: Config) = config.apply {
        get("theme")?.toIntOrNull()?.let {
            ui.themeColor = when (it) {
                // Light, Dark, Black, Red, Amber
                0, 1, 2, 13, 16 -> DEFAULT
                // Chocolate, DarkRed, LightRed
                3, 12, 14 -> RED
                // LightYellow, DarkGreen, LightGreen
                5, 15, 17 -> GREEN
                // Indigo, Blue, LightBlue
                4, 7, 8 -> BLUE
                // DarkPurple, Purple, LightPurple
                9, 10, 11 -> PURPLE
                // DarkBlue
                6 -> TEAL
                else -> DEFAULT
            }
            ui.themeType = M3
            ui.themeMode = when (it) {
                // Light, Dark, Black, Red, Amber
                0, 1, 2, 13, 16 -> DAYNIGHT
                // Chocolate, Blue, Purple
                3, 7, 10 -> FULL
                // Indigo, LightYellow, DarkBlue, LightBlue, DarkPurple, LightPurple,
                // DarkRed, LightRed, DarkGreen, LightGreen
                4, 5, 6, 8, 9, 11, 12, 14, 15, 17 -> CLASSIC
                else -> DAYNIGHT
            }
            ui.themeNightMode = when (it) {
                // Light, LightYellow, LightBlue, LightPurple, LightRed, LightGreen
                0, 5, 8, 11, 14, 17 -> false
                // Black, Indigo, DarkBlue, DarkPurple, DarkRed, DarkGreen
                2, 4, 6, 9, 12, 15 -> true
                // Dark, Red, Amber
                1, 13, 16 -> null
                else -> null
            }
            ui.themeBlackMode = it == 2
        }

        get("miniMenuButtons")?.toJsonArray()?.let {
            try {
                ui.miniMenuButtons = it.map { id -> NavTarget.getById(id.asInt) }.toSet()
            } catch (e: Exception) {
                ui.miniMenuButtons = NavTarget.getDefaultConfig()
            }
        }
    }
}
