/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-1.
 */

package pl.szczodrzynski.edziennik.config.utils

import pl.szczodrzynski.edziennik.config.ProfileConfig
import pl.szczodrzynski.edziennik.data.db.entity.Profile.Companion.AGENDA_DEFAULT
import pl.szczodrzynski.edziennik.data.db.enums.NotificationType
import pl.szczodrzynski.edziennik.data.db.enums.SchoolType
import pl.szczodrzynski.edziennik.ui.home.HomeCard
import pl.szczodrzynski.edziennik.ui.home.HomeCardModel
import pl.szczodrzynski.edziennik.utils.managers.GradesManager.Companion.COLOR_MODE_WEIGHTED
import pl.szczodrzynski.edziennik.utils.managers.GradesManager.Companion.YEAR_ALL_GRADES

class ProfileConfigMigration(config: ProfileConfig) {
    init { config.apply {

        if (dataVersion < 2) {
            sync.notificationFilter = sync.notificationFilter + NotificationType.TEACHER_ABSENCE

            dataVersion = 2
        }

        if (dataVersion < 3) {
            if (ui.homeCards.isNotEmpty()) {
                ui.homeCards = ui.homeCards + HomeCardModel(
                    profileId = config.profileId ?: -1,
                    cardId = HomeCard.CARD_NOTES,
                )
            }

            dataVersion = 3
        }

        if (dataVersion < 4) {
            // switch to new event types (USOS)
            dataVersion = 4

            val profile = db.profileDao().getByIdNow(profileId ?: -1)
            if (profile?.loginStoreType?.schoolType == SchoolType.UNIVERSITY) {
                db.eventTypeDao().clear(profileId ?: -1)
                db.eventTypeDao().addDefaultTypes(profile)
            }
        }
    }}
}
