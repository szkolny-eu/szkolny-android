/*
 * Copyright (c) Kuba Szczodrzyński 2021-2-21.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.hebe

import pl.szczodrzynski.edziennik.data.api.VULCAN_HEBE_ENDPOINT_HOMEWORK
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_HEBE_HOMEWORK
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanHebe
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.enums.MetadataType
import pl.szczodrzynski.edziennik.ext.asJsonObjectList
import pl.szczodrzynski.edziennik.ext.getJsonArray
import pl.szczodrzynski.edziennik.ext.getLong
import pl.szczodrzynski.edziennik.ext.getString
import pl.szczodrzynski.edziennik.utils.Utils

class VulcanHebeHomework(
    override val data: DataVulcan,
    override val lastSync: Long?,
    val onSuccess: (endpointId: Int) -> Unit
) : VulcanHebe(data, lastSync) {
    companion object {
        const val TAG = "VulcanHebeHomework"
    }

    init {
        apiGetList(
            TAG,
            VULCAN_HEBE_ENDPOINT_HOMEWORK,
            HebeFilterType.BY_PUPIL,
            lastSync = lastSync
        ) { list, _ ->
            list.forEach { exam ->
                val id = exam.getLong("IdHomework") ?: return@forEach
                val eventDate = getDate(exam, "Deadline") ?: return@forEach
                val subjectId = getSubjectId(exam, "Subject") ?: -1
                val teacherId = getTeacherId(exam, "Creator") ?: -1
                val teamId = data.teamClass?.id ?: -1
                val topic = exam.getString("Content")?.trim() ?: ""

                if (!isCurrentYear(eventDate)) return@forEach

                val lessonList = data.db.timetableDao().getAllForDateNow(profileId, eventDate)
                val startTime = lessonList.firstOrNull { it.subjectId == subjectId }?.startTime

                val eventObject = Event(
                    profileId = profileId,
                    id = id,
                    date = eventDate,
                    time = startTime,
                    topic = topic,
                    color = null,
                    type = Event.TYPE_HOMEWORK,
                    teacherId = teacherId,
                    subjectId = subjectId,
                    teamId = teamId
                )

                val attachments = exam.getJsonArray("Attachments")
                    ?.asJsonObjectList()
                    ?: return@forEach

                for (attachment in attachments) {
                    val fileName = attachment.getString("Name") ?: continue
                    val url = attachment.getString("Link") ?: continue
                    val attachmentName = "$fileName:$url"
                    val attachmentId = Utils.crc32(attachmentName.toByteArray())

                    eventObject.addAttachment(
                        id = attachmentId,
                        name = attachmentName
                    )
                }

                data.eventList.add(eventObject)
                data.metadataList.add(
                    Metadata(
                        profileId,
                        MetadataType.HOMEWORK,
                        id,
                        profile?.empty ?: true,
                        profile?.empty ?: true
                    )
                )
            }

            data.setSyncNext(ENDPOINT_VULCAN_HEBE_HOMEWORK, SYNC_ALWAYS)
            onSuccess(ENDPOINT_VULCAN_HEBE_HOMEWORK)
        }
    }
}
