/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-26
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.web

import org.jsoup.Jsoup
import pl.szczodrzynski.edziennik.crc32
import pl.szczodrzynski.edziennik.data.api.Regexes.EDUDZIENNIK_ANNOUNCEMENT_ID
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.DataEdudziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.ENDPOINT_EDUDZIENNIK_WEB_ANNOUNCEMENTS
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.EdudziennikWeb
import pl.szczodrzynski.edziennik.data.db.entity.Announcement
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.get
import pl.szczodrzynski.edziennik.utils.models.Date

class EdudziennikWebAnnouncements(override val data: DataEdudziennik,
                                  val onSuccess: () -> Unit) : EdudziennikWeb(data) {
    companion object {
        const val TAG = "EdudziennikWebAnnouncements"
    }

    init { data.profile?.also { profile ->
        webGet(TAG, data.schoolClassEndpoint + "Announcements") { text ->
            val doc = Jsoup.parse(text)

            if (doc.getElementsByClass("message").text().trim() != "Brak ogłoszeń.") {
                doc.select("table.list tbody tr").forEach { announcementElement ->
                    val titleElement = announcementElement.child(0).child(0)

                    val longId = EDUDZIENNIK_ANNOUNCEMENT_ID.find(titleElement.attr("href"))?.get(1)
                            ?: return@forEach
                    val id = longId.crc32()
                    val subject = titleElement.text()

                    val teacherName = announcementElement.child(1).text()
                    val teacher = data.getTeacherByFirstLast(teacherName)

                    val dateString = announcementElement.getElementsByClass("datetime").first().text()
                    val startDate = Date.fromY_m_d(dateString)
                    val addedDate = Date.fromIsoHm(dateString)

                    val announcementObject = Announcement(
                            profileId,
                            id,
                            subject,
                            null,
                            startDate,
                            null,
                            teacher.id,
                            longId
                    )

                    data.announcementIgnoreList.add(announcementObject)
                    data.metadataList.add(Metadata(
                            profileId,
                            Metadata.TYPE_ANNOUNCEMENT,
                            id,
                            profile.empty,
                            profile.empty,
                            addedDate
                    ))
                }
            }

            data.setSyncNext(ENDPOINT_EDUDZIENNIK_WEB_ANNOUNCEMENTS, SYNC_ALWAYS)
            onSuccess()
        }
    } ?: onSuccess() }
}
