/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-1.
 */

package pl.szczodrzynski.edziennik.config.utils

import pl.szczodrzynski.edziennik.config.ProfileConfig
import pl.szczodrzynski.edziennik.data.db.entity.Profile.Companion.AGENDA_DEFAULT
import pl.szczodrzynski.edziennik.data.db.enums.NotificationType
import pl.szczodrzynski.edziennik.ui.home.HomeCard
import pl.szczodrzynski.edziennik.ui.home.HomeCardModel
import pl.szczodrzynski.edziennik.utils.managers.GradesManager.Companion.COLOR_MODE_WEIGHTED
import pl.szczodrzynski.edziennik.utils.managers.GradesManager.Companion.YEAR_ALL_GRADES

class ProfileConfigMigration(config: ProfileConfig) {
    init { config.apply {

        if (dataVersion < 1) {
            grades.colorMode = COLOR_MODE_WEIGHTED
            grades.yearAverageMode = YEAR_ALL_GRADES
            grades.hideImproved = false
            grades.averageWithoutWeight = true
            grades.plusValue = null
            grades.minusValue = null
            grades.dontCountEnabled = false
            grades.dontCountGrades = listOf()
            ui.agendaViewType = AGENDA_DEFAULT
            // no migration for ui.homeCards

            dataVersion = 1
        }

        if (dataVersion < 2) {
            sync.notificationFilter = sync.notificationFilter + NotificationType.TEACHER_ABSENCE

            dataVersion = 2
        }

        if (dataVersion < 3) {
            if (ui.homeCards.isNotEmpty()) {
                ui.homeCards = ui.homeCards.toMutableList().also {
                    it.add(HomeCardModel(config.profileId, HomeCard.CARD_NOTES))
                }
            }

            dataVersion = 3
        }
    }}
}
