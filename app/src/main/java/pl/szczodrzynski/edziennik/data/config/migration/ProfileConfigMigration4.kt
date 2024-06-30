/*
 * Copyright (c) Kuba Szczodrzyński 2024-6-27.
 */

package pl.szczodrzynski.edziennik.data.config.migration

import pl.szczodrzynski.edziennik.data.config.BaseMigration
import pl.szczodrzynski.edziennik.data.config.ProfileConfig
import pl.szczodrzynski.edziennik.data.enums.SchoolType

class ProfileConfigMigration4 : BaseMigration<ProfileConfig>() {

    override fun migrate(config: ProfileConfig) = config.apply {
        // switch to new event types (USOS)
        if (profile?.loginStoreType?.schoolType == SchoolType.UNIVERSITY) {
            db.eventTypeDao().clear(profileId ?: -1)
            db.eventTypeDao().addDefaultTypes(profile!!)
        }
    }
}
