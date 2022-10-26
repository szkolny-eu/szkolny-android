/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-27.
 */

package pl.szczodrzynski.edziennik.config.utils

import android.content.Context
import androidx.core.content.edit
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.config.Config
import pl.szczodrzynski.edziennik.utils.models.Time
import kotlin.math.abs

class ConfigMigration(app: App, config: Config) {
    init { config.apply {

        val p = app.getSharedPreferences("pl.szczodrzynski.edziennik_profiles", Context.MODE_PRIVATE)
        if (p.contains("app.appConfig.appTheme")) {
            // migrate appConfig from app version 3.x and lower.
            // Updates dataVersion to level 2.
            AppConfigMigrationV3(p, config)
            p.edit {
                remove("app.appConfig.appTheme")
            }
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

        hash = "invalid"
    }}
}
