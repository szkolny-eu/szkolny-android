/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-23
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.web

import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.DataEdudziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.ENDPOINT_EDUDZIENNIK_WEB_LUCKY_NUMBER
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.EdudziennikWeb
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.modules.luckynumber.LuckyNumber
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.utils.models.Date

class EdudziennikWebLuckyNumber(override val data: DataEdudziennik,
                                val onSuccess: () -> Unit) : EdudziennikWeb(data) {
    companion object {
        private const val TAG = "EdudziennikWebLuckyNumber"
    }

    init { data.profile?.also { profile ->
        webGet(TAG, data.schoolEndpoint + "Lucky", xhr = true) { text ->
            val luckyNumber = text.toInt()

            val luckyNumberObject = LuckyNumber(
                    profileId,
                    Date.getToday(),
                    luckyNumber
            )

            data.luckyNumberList.add(luckyNumberObject)
            data.metadataList.add(Metadata(
                    profileId,
                    Metadata.TYPE_LUCKY_NUMBER,
                    luckyNumberObject.date.value.toLong(),
                    profile.empty,
                    profile.empty,
                    System.currentTimeMillis()
            ))

            data.setSyncNext(ENDPOINT_EDUDZIENNIK_WEB_LUCKY_NUMBER, SYNC_ALWAYS)
            onSuccess()
        }
    } ?: onSuccess() }
}
