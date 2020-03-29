/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-1.
 */

package pl.szczodrzynski.edziennik.config.utils

import pl.szczodrzynski.edziennik.config.ProfileConfig
import pl.szczodrzynski.edziennik.data.db.entity.Notification
import pl.szczodrzynski.edziennik.data.db.entity.Profile.Companion.AGENDA_DEFAULT
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
            sync.notificationFilter = sync.notificationFilter + Notification.TYPE_TEACHER_ABSENCE

            dataVersion = 2
        }
    }}
}
