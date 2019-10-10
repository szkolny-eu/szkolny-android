/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-10.
 */

package pl.szczodrzynski.edziennik.api.v2.mobidziennik.data.web

import pl.szczodrzynski.edziennik.api.v2.Regexes
import pl.szczodrzynski.edziennik.api.v2.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.db.modules.luckynumber.LuckyNumber
import pl.szczodrzynski.edziennik.utils.models.Date

class MobidziennikLuckyNumberExtractor(val data: DataMobidziennik, text: String) {
    init {
        data.profile?.luckyNumber = -1
        data.profile?.luckyNumberDate = Date.getToday()

        Regexes.MOBIDZIENNIK_LUCKY_NUMBER.find(text)?.let {
            try {
                val luckyNumber = it.groupValues[1].toInt()

                data.profile?.luckyNumber = luckyNumber
                data.profile?.luckyNumberDate = Date.getToday()
                data.luckyNumberList.add(
                        LuckyNumber(
                                data.profileId,
                                Date.getToday(),
                                luckyNumber
                        ))
            } catch (_: Exception){}
        }
    }
}