/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-1.
 */

package pl.szczodrzynski.edziennik.config.utils

import pl.szczodrzynski.edziennik.config.ProfileConfig
import pl.szczodrzynski.edziennik.data.db.entity.Profile.Companion.AGENDA_DEFAULT
import pl.szczodrzynski.edziennik.data.db.entity.Profile.Companion.COLOR_MODE_WEIGHTED
import pl.szczodrzynski.edziennik.data.db.entity.Profile.Companion.YEAR_ALL_GRADES

class ProfileConfigMigration(config: ProfileConfig) {
    init { config.apply {

        if (dataVersion < 1) {
            grades.colorMode = COLOR_MODE_WEIGHTED
            grades.countZeroToAvg = true
            grades.yearAverageMode = YEAR_ALL_GRADES
            ui.agendaViewType = AGENDA_DEFAULT

            dataVersion = 1
        }
    }}
}