/*
 * Copyright (c) Kuba Szczodrzyński 2021-9-8.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.web

import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.ENDPOINT_MOBIDZIENNIK_WEB_NOTICES
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Week

class MobidziennikWebTimetable(
    override val data: DataMobidziennik,
    override val lastSync: Long?,
    val onSuccess: (endpointId: Int) -> Unit
) : MobidziennikWeb(data, lastSync) {
    companion object {
        private const val TAG = "MobidziennikWebTimetable"
    }

    private fun parseCss(css: String): Map<String, String> {
        return css.split(";").mapNotNull {
            val spl = it.split(":")
            if (spl.size != 2)
                return@mapNotNull null
            return@mapNotNull spl[0].trim() to spl[1].trim()
        }.toMap()
    }

    init {
        val currentWeekStart = Week.getWeekStart()
        if (Date.getToday().weekDay > 4) {
            currentWeekStart.stepForward(0, 0, 7)
        }
        val startDateStr = data.arguments?.getString("weekStart") ?: currentWeekStart.stringY_m_d

        webGet(TAG, "/dziennik/planlekcji?typ=podstawowy&tydzien=$startDateStr") { html ->
            MobidziennikLuckyNumberExtractor(data, html)


        }
    }
}

