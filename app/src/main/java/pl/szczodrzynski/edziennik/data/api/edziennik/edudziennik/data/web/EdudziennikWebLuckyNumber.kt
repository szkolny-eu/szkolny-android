/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-23
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.web

import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.DataEdudziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.ENDPOINT_EDUDZIENNIK_WEB_LUCKY_NUMBER
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.EdudziennikWeb
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
            text.toIntOrNull()?.also { luckyNumber ->
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
            }

            val nextSync = Date.getToday().stepForward(0, 0, 1).inMillis

            data.setSyncNext(ENDPOINT_EDUDZIENNIK_WEB_LUCKY_NUMBER, syncAt = nextSync)
            onSuccess()
        }
    } ?: onSuccess() }
}
