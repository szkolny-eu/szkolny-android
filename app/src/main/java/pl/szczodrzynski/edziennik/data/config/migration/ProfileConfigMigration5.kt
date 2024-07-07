/*
 * Copyright (c) Kuba Szczodrzyński 2024-6-27.
 */

package pl.szczodrzynski.edziennik.data.config.migration

import pl.szczodrzynski.edziennik.data.config.BaseMigration
import pl.szczodrzynski.edziennik.data.config.ProfileConfig
import pl.szczodrzynski.edziennik.data.enums.SchoolType

class ProfileConfigMigration5 : BaseMigration<ProfileConfig>() {

    override fun migrate(config: ProfileConfig) = config.apply {
        // update USOS event types and the appropriate events (2022-12-25)
        if (profile?.loginStoreType?.schoolType == SchoolType.UNIVERSITY) {
            db.eventTypeDao().getAllWithDefaults(profile!!)
            // wejściówka (4) -> kartkówka (3)
            db.eventDao().getRawNow("UPDATE events SET eventType = 3 WHERE profileId = $profileId AND eventType = 4;")
            // zadanie (6) -> zadanie domowe (-1)
            db.eventDao().getRawNow("UPDATE events SET eventType = -1 WHERE profileId = $profileId AND eventType = 6;")
        }
    }
}
