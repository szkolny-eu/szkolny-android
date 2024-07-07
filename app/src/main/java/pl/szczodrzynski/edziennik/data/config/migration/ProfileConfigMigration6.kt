/*
 * Copyright (c) Kuba Szczodrzyński 2024-6-28.
 */

package pl.szczodrzynski.edziennik.data.config.migration

import pl.szczodrzynski.edziennik.data.config.BaseMigration
import pl.szczodrzynski.edziennik.data.config.ProfileConfig
import pl.szczodrzynski.edziennik.data.enums.NotificationType
import pl.szczodrzynski.edziennik.ext.toJsonArray

class ProfileConfigMigration6 : BaseMigration<ProfileConfig>() {

    override fun migrate(config: ProfileConfig) = config.apply {
        get("notificationFilter")?.toJsonArray()?.let { notificationFilter ->
            try {
                sync.notificationFilter = notificationFilter.map { id ->
                    NotificationType.entries.first { it.id == id.asInt }
                }.toSet()
            } catch (e: Exception) {
                sync.notificationFilter = NotificationType.getDefaultConfig()
            }
        }
    }
}
