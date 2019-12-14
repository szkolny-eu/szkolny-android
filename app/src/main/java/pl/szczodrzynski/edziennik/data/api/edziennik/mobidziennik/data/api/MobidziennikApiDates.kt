/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-11.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.api

import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.utils.models.Date

class MobidziennikApiDates(val data: DataMobidziennik, rows: List<String>) {
    init {
        for (row in rows) {
            if (row.isEmpty())
                continue
            val cols = row.split("|")

            when (cols[1]) {
                "semestr1_poczatek" -> data.profile?.dateSemester1Start = Date.fromYmd(cols[3])
                "semestr2_poczatek" -> data.profile?.dateSemester2Start = Date.fromYmd(cols[3])
                "koniec_roku_szkolnego" -> data.profile?.dateYearEnd = Date.fromYmd(cols[3])
            }
        }
    }
}
