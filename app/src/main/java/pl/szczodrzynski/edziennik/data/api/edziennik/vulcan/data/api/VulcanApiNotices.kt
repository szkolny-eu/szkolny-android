/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-23
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.api

import androidx.core.util.isEmpty
import pl.szczodrzynski.edziennik.data.api.VULCAN_API_ENDPOINT_NOTICES
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_API_NOTICES
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanApi
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.Notice
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.getJsonArray
import pl.szczodrzynski.edziennik.getLong
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.toSparseArray

class VulcanApiNotices(override val data: DataVulcan,
                       override val lastSync: Long?,
                       val onSuccess: (endpointId: Int) -> Unit
) : VulcanApi(data, lastSync) {
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
                val addedDate = notice.getLong("DataModyfikacji")?.times(1000) ?: System.currentTimeMillis()

                val categoryId = notice.getLong("IdKategoriaUwag") ?: -1
                val categoryText = data.noticeTypes[categoryId]?.name ?: ""

                val noticeObject = Notice(
                        profileId = profileId,
                        id = id,
                        type = Notice.TYPE_NEUTRAL,
                        semester = profile.currentSemester,
                        text = text,
                        category = categoryText,
                        points = null,
                        teacherId = teacherId,
                        addedDate = addedDate
                )

                data.noticeList.add(noticeObject)
                data.metadataList.add(Metadata(
                        profileId,
                        Metadata.TYPE_NOTICE,
                        id,
                        profile.empty,
                        profile.empty
                ))
            }

            data.setSyncNext(ENDPOINT_VULCAN_API_NOTICES, SYNC_ALWAYS)
            onSuccess(ENDPOINT_VULCAN_API_NOTICES)
        }
    } ?: onSuccess(ENDPOINT_VULCAN_API_NOTICES) }
}
