/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-23
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.web

import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.DataEdudziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.ENDPOINT_EDUDZIENNIK_WEB_LUCKY_NUMBER
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.EdudziennikWeb
import pl.szczodrzynski.edziennik.data.db.entity.LuckyNumber
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.utils.models.Date

class EdudziennikWebLuckyNumber(override val data: DataEdudziennik,
                                override val lastSync: Long?,
                                val onSuccess: (endpointId: Int) -> Unit
) : EdudziennikWeb(data, lastSync) {
    companion object {
        private const val TAG = "EdudziennikWebLuckyNumber"
    }

    init { data.profile?.also { profile ->
        webGet(TAG, data.schoolEndpoint + "Lucky", xhr = true) { text ->
            text.toIntOrNull()?.also { luckyNumber ->
                val luckyNumberObject = LuckyNumber(
                        profileId = profileId,
                        date = Date.getToday(),
                        number = luckyNumber
                )

                data.luckyNumberList.add(luckyNumberObject)
                data.metadataList.add(Metadata(
                        profileId,
                        Metadata.TYPE_LUCKY_NUMBER,
                        luckyNumberObject.date.value.toLong(),
                        true,
                        profile.empty
                ))
            }

            data.setSyncNext(ENDPOINT_EDUDZIENNIK_WEB_LUCKY_NUMBER, SYNC_ALWAYS)
            onSuccess(ENDPOINT_EDUDZIENNIK_WEB_LUCKY_NUMBER)
        }
    } ?: onSuccess(ENDPOINT_EDUDZIENNIK_WEB_LUCKY_NUMBER) }
}
