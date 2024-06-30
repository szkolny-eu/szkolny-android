/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-27.
 */

package pl.szczodrzynski.edziennik.data.config.migration

import pl.szczodrzynski.edziennik.data.config.BaseMigration
import pl.szczodrzynski.edziennik.data.config.Config
import pl.szczodrzynski.edziennik.utils.models.Time
import kotlin.math.abs

class ConfigMigration11 : BaseMigration<Config>() {

    override fun migrate(config: Config) = config.apply {
        val startMillis = this["quietHoursStart"]?.toLongOrNull() ?: 0L
        val endMillis = this["quietHoursEnd"]?.toLongOrNull() ?: 0L
        if (startMillis > 0) {
            try {
                sync.quietHoursStart = Time.fromMillis(abs(startMillis))
                sync.quietHoursEnd = Time.fromMillis(abs(endMillis))
                sync.quietHoursEnabled = true
            } catch (_: Exception) {
            }
        } else {
            sync.quietHoursEnabled = false
            sync.quietHoursStart = null
            sync.quietHoursEnd = null
        }
    }
}
