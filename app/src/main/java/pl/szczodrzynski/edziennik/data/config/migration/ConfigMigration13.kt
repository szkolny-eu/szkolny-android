/*
 * Copyright (c) Kuba Szczodrzyński 2024-6-28.
 */

package pl.szczodrzynski.edziennik.data.config.migration

import android.app.UiModeManager.MODE_NIGHT_NO
import android.app.UiModeManager.MODE_NIGHT_YES
import pl.szczodrzynski.edziennik.data.config.BaseMigration
import pl.szczodrzynski.edziennik.data.config.Config
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import pl.szczodrzynski.edziennik.data.enums.Theme.*
import pl.szczodrzynski.edziennik.data.enums.Theme.Mode.BLACK
import pl.szczodrzynski.edziennik.data.enums.Theme.Mode.CLASSIC
import pl.szczodrzynski.edziennik.data.enums.Theme.Mode.FULL
import pl.szczodrzynski.edziennik.ext.toJsonArray

class ConfigMigration13 : BaseMigration<Config>() {

    override fun migrate(config: Config) = config.apply {
        get("theme")?.toIntOrNull()?.let {
            ui.themeConfig = when (it) {
                // Light
                // Dark
                // Red
                // Amber
                0, 1, 13, 16 -> Config()
                // Black
                2 -> Config(mode = BLACK)
                // Chocolate
                3 -> Config(color = RED, mode = FULL)
                // Indigo
                4 -> Config(color = BLUE, mode = CLASSIC, nightMode = MODE_NIGHT_YES)
                // DarkBlue
                6 -> Config(color = TEAL, mode = CLASSIC, nightMode = MODE_NIGHT_YES)
                // Blue
                7 -> Config(color = BLUE, mode = FULL)
                // LightBlue
                8 -> Config(color = BLUE, mode = CLASSIC, nightMode = MODE_NIGHT_NO)
                // DarkPurple
                9 -> Config(color = PURPLE, mode = CLASSIC, nightMode = MODE_NIGHT_YES)
                // Purple
                10 -> Config(color = PURPLE, mode = FULL)
                // LightPurple
                11 -> Config(color = PURPLE, mode = CLASSIC, nightMode = MODE_NIGHT_NO)
                // DarkRed
                12 -> Config(color = RED, mode = CLASSIC, nightMode = MODE_NIGHT_YES)
                // LightRed
                14 -> Config(color = RED, mode = CLASSIC, nightMode = MODE_NIGHT_NO)
                // DarkGreen
                15 -> Config(color = GREEN, mode = CLASSIC, nightMode = MODE_NIGHT_YES)
                // LightYellow
                // LightGreen
                5, 17 -> Config(color = GREEN, mode = CLASSIC, nightMode = MODE_NIGHT_NO)

                else -> Config()
            }
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
