/*
 * Copyright (c) Kacper Ziubryniewicz 2021-2-22
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.hebe

import pl.szczodrzynski.edziennik.data.api.VULCAN_HEBE_ENDPOINT_NOTICES
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_HEBE_NOTICES
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanHebe
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.Notice
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.ext.*

class VulcanHebeNotices(
    override val data: DataVulcan,
    override val lastSync: Long?,
    val onSuccess: (endpointId: Int) -> Unit,
) : VulcanHebe(data, lastSync) {

    companion object {
        const val TAG = "VulcanHebeNotices"
    }

    init {
        apiGetList(
            TAG,
            VULCAN_HEBE_ENDPOINT_NOTICES,
            HebeFilterType.BY_PUPIL,
            lastSync = lastSync
        ) { list, _ ->
            list.forEach { notice ->
                val id = notice.getLong("Id") ?: return@forEach
                val type = when (notice.getBoolean("Positive")) {
                    true -> Notice.TYPE_POSITIVE
                    else -> Notice.TYPE_NEUTRAL
                }
                val date = getDate(notice, "DateValid") ?: return@forEach
                val semester = profile?.dateToSemester(date) ?: return@forEach
                val text = notice.getString("Content") ?: ""
                val category = notice.getJsonObject("Category")?.getString("Name")
                val points = notice.getFloat("Points")
                val teacherId = getTeacherId(notice, "Creator") ?: -1
                val addedDate = getDateTime(notice, "DateModify")

                if (!isCurrentYear(date)) return@forEach

                val noticeObject = Notice(
                    profileId = profileId,
                    id = id,
                    type = type,
                    semester = semester,
                    text = text,
                    category = category,
                    points = points,
                    teacherId = teacherId,
                    addedDate = addedDate
                )

                data.noticeList.add(noticeObject)
                data.metadataList.add(
                    Metadata(
                        profileId,
                        Metadata.TYPE_NOTICE,
                        id,
                        profile?.empty ?: true,
                        profile?.empty ?: true
                    )
                )
            }

            data.setSyncNext(ENDPOINT_VULCAN_HEBE_NOTICES, SYNC_ALWAYS)
            onSuccess(ENDPOINT_VULCAN_HEBE_NOTICES)
        }
    }
}
