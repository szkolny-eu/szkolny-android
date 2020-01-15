/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-1
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.web

import org.jsoup.Jsoup
import pl.szczodrzynski.edziennik.crc32
import pl.szczodrzynski.edziennik.data.api.Regexes.EDUDZIENNIK_NOTE_ID
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.DataEdudziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.ENDPOINT_EDUDZIENNIK_WEB_NOTES
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.EdudziennikWeb
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.Notice
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.get
import pl.szczodrzynski.edziennik.utils.models.Date

class EdudziennikWebNotes(override val data: DataEdudziennik,
                          val onSuccess: () -> Unit) : EdudziennikWeb(data) {
    companion object {
        const val TAG = "EdudziennikWebNotes"
    }

    init { data.profile?.also { profile ->
        webGet(TAG, data.classStudentEndpoint + "RegistryNotesStudent", xhr = true) { text ->
            val doc = Jsoup.parseBodyFragment("<table>" + text.trim() + "</table>")

            doc.getElementsByTag("tr").forEach { noteElement ->
                val dateElement = noteElement.getElementsByClass("date").first().child(0)
                val addedDate = Date.fromY_m_d(dateElement.text()).inMillis

                val id = EDUDZIENNIK_NOTE_ID.find(dateElement.attr("href"))?.get(0)?.crc32()
                        ?: return@forEach

                val teacherName = noteElement.child(1).text()
                val teacher = data.getTeacherByFirstLast(teacherName)

                val description = noteElement.child(3).text()

                val noticeObject = Notice(
                        profileId,
                        id,
                        description,
                        profile.currentSemester,
                        Notice.TYPE_NEUTRAL,
                        teacher.id
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

            data.setSyncNext(ENDPOINT_EDUDZIENNIK_WEB_NOTES, SYNC_ALWAYS)
            onSuccess()
        }
    } ?: onSuccess() }
}
