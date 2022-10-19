/*
 * Copyright (c) Kacper Ziubryniewicz 2020-5-13
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.data.api

import pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.DataPodlasie
import pl.szczodrzynski.edziennik.data.db.entity.LuckyNumber
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.enums.MetadataType
import pl.szczodrzynski.edziennik.utils.models.Date

class PodlasieApiLuckyNumber(val data: DataPodlasie, val luckyNumber: Int) {
    init {
        val luckyNumberObject = LuckyNumber(
                profileId = data.profileId,
                date = Date.getToday(),
                number = luckyNumber
        )

        data.luckyNumberList.add(luckyNumberObject)
        data.metadataList.add(
                Metadata(
                        data.profileId,
                        MetadataType.LUCKY_NUMBER,
                        luckyNumberObject.date.value.toLong(),
                        true,
                        data.profile?.empty ?: false
                ))
    }
}
