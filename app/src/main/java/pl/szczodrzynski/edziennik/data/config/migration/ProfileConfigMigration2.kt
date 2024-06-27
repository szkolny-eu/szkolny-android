/*
 * Copyright (c) Kuba Szczodrzyński 2024-6-27.
 */

package pl.szczodrzynski.edziennik.data.config.migration

import pl.szczodrzynski.edziennik.data.config.BaseMigration
import pl.szczodrzynski.edziennik.data.config.ProfileConfig
import pl.szczodrzynski.edziennik.data.enums.NotificationType

class ProfileConfigMigration2 : BaseMigration<ProfileConfig>() {

    override fun migrate(config: ProfileConfig) = config.apply {
        sync.notificationFilter += NotificationType.TEACHER_ABSENCE
    }
}
