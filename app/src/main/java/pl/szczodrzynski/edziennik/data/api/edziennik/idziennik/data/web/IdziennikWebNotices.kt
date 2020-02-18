/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-28.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.web

import pl.szczodrzynski.edziennik.crc16
import pl.szczodrzynski.edziennik.data.api.ERROR_IDZIENNIK_WEB_REQUEST_NO_DATA
import pl.szczodrzynski.edziennik.data.api.IDZIENNIK_WEB_NOTICES
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.DataIdziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.ENDPOINT_IDZIENNIK_WEB_NOTICES
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.IdziennikWeb
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.Notice
import pl.szczodrzynski.edziennik.data.db.entity.Notice.*
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.getJsonObject
import pl.szczodrzynski.edziennik.utils.models.Date

class IdziennikWebNotices(override val data: DataIdziennik,
                          override val lastSync: Long?,
                          val onSuccess: (endpointId: Int) -> Unit
) : IdziennikWeb(data, lastSync) {
    companion object {
        private const val TAG = "IdziennikWebNotices"
    }

    init {
        webApiGet(TAG, IDZIENNIK_WEB_NOTICES, mapOf(
                "idPozDziennika" to data.registerId
        )) { result ->
            val json = result.getJsonObject("d") ?: run {
                data.error(ApiError(TAG, ERROR_IDZIENNIK_WEB_REQUEST_NO_DATA)
                        .withApiResponse(result))
                return@webApiGet
            }

            for (jNoticeEl in json.getAsJsonArray("SUwaga")) {
                val jNotice = jNoticeEl.asJsonObject
                // jNotice
                val noticeId = jNotice.get("id").asString.crc16().toLong()

                val rTeacher = data.getTeacherByLastFirst(jNotice.get("Nauczyciel").asString)
                val addedDate = Date.fromY_m_d(jNotice.get("Data").asString)

                var nType = TYPE_NEUTRAL
                val jType = jNotice.get("Typ").asString
                if (jType == "n") {
                    nType = TYPE_NEGATIVE
                } else if (jType == "p") {
                    nType = TYPE_POSITIVE
                }

                val noticeObject = Notice(
                        profileId,
                        noticeId,
                        jNotice.get("Tresc").asString,
                        jNotice.get("Semestr").asInt,
                        nType,
                        rTeacher.id)
                data.noticeList.add(noticeObject)
                data.metadataList.add(Metadata(
                        profileId,
                        Metadata.TYPE_NOTICE,
                        noticeObject.id,
                        profile?.empty ?: false,
                        profile?.empty ?: false,
                        addedDate.inMillis
                ))
            }

            data.setSyncNext(ENDPOINT_IDZIENNIK_WEB_NOTICES, SYNC_ALWAYS)
            onSuccess(ENDPOINT_IDZIENNIK_WEB_NOTICES)
        }
    }
}
