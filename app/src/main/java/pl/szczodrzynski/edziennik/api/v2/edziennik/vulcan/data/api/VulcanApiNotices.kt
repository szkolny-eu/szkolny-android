/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-23
 */

package pl.szczodrzynski.edziennik.api.v2.edziennik.vulcan.data.api

import androidx.core.util.isEmpty
import pl.szczodrzynski.edziennik.api.v2.VULCAN_API_ENDPOINT_NOTICES
import pl.szczodrzynski.edziennik.api.v2.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.api.v2.edziennik.vulcan.ENDPOINT_VULCAN_API_NOTICES
import pl.szczodrzynski.edziennik.api.v2.edziennik.vulcan.data.VulcanApi
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.data.db.modules.notices.Notice
import pl.szczodrzynski.edziennik.getJsonArray
import pl.szczodrzynski.edziennik.getLong
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.toSparseArray
import pl.szczodrzynski.edziennik.utils.models.Date

class VulcanApiNotices(override val data: DataVulcan, val onSuccess: () -> Unit) : VulcanApi(data) {
    companion object {
        const val TAG = "VulcanApiNotices"
    }

    init { data.profile?.also { profile ->
        if (data.noticeTypes.isEmpty()) {
            data.db.noticeTypeDao().getAllNow(profileId).toSparseArray(data.noticeTypes) { it.id }
        }

        apiGet(TAG, VULCAN_API_ENDPOINT_NOTICES, parameters = mapOf(
                "IdUczen" to data.studentId,
                "IdOkresKlasyfikacyjny" to data.studentSemesterId
        )) { json, _ ->
            json.getJsonArray("Data")?.forEach { noticeEl ->
                val notice = noticeEl.asJsonObject

                val id = notice.getLong("Id") ?: return@forEach
                val text = notice.getString("TrescUwagi") ?: return@forEach
                val teacherId = notice.getLong("IdPracownik") ?: -1
                val addedDate = Date.fromY_m_d(notice.getString("DataWpisuTekst")).inMillis

                val noticeObject = Notice(
                        profileId,
                        id,
                        text,
                        profile.currentSemester,
                        Notice.TYPE_NEUTRAL,
                        teacherId
                )

                data.noticeList.add(noticeObject)
                data.metadataList.add(Metadata(
                        profileId,
                        Metadata.TYPE_NOTICE,
                        id,
                        profile.empty,
                        profile.empty,
                        addedDate
                ))
            }

            data.setSyncNext(ENDPOINT_VULCAN_API_NOTICES, SYNC_ALWAYS)
            onSuccess()
        }
    } ?: onSuccess()}
}
