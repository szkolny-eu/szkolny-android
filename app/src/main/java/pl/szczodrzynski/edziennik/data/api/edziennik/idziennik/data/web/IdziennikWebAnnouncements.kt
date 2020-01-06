/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-28.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.web

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.data.api.ERROR_IDZIENNIK_WEB_REQUEST_NO_DATA
import pl.szczodrzynski.edziennik.data.api.IDZIENNIK_WEB_ANNOUNCEMENTS
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.DataIdziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.ENDPOINT_IDZIENNIK_WEB_ANNOUNCEMENTS
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.IdziennikWeb
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.entity.Announcement
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.getJsonObject
import pl.szczodrzynski.edziennik.utils.models.Date

class IdziennikWebAnnouncements(override val data: DataIdziennik,
                          val onSuccess: () -> Unit) : IdziennikWeb(data) {
    companion object {
        private const val TAG = "IdziennikWebAnnouncements"
    }

    init {
        val param = JsonObject()
        param.add("parametryFiltrow", JsonArray())

        webApiGet(TAG, IDZIENNIK_WEB_ANNOUNCEMENTS, mapOf(
                "uczenId" to (data.studentId ?: ""),
                "param" to param
        )) { result ->
            val json = result.getJsonObject("d") ?: run {
                data.error(ApiError(TAG, ERROR_IDZIENNIK_WEB_REQUEST_NO_DATA)
                        .withApiResponse(result))
                return@webApiGet
            }

            for (jAnnouncementEl in json.getAsJsonArray("ListK")) {
                val jAnnouncement = jAnnouncementEl.asJsonObject
                // jAnnouncement
                val announcementId = jAnnouncement.get("Id").asLong

                val rTeacher = data.getTeacherByFirstLast(jAnnouncement.get("Autor").asString)
                val addedDate = java.lang.Long.parseLong(jAnnouncement.get("DataDodania").asString.replace("[^\\d]".toRegex(), ""))
                val startDate = Date.fromMillis(java.lang.Long.parseLong(jAnnouncement.get("DataWydarzenia").asString.replace("[^\\d]".toRegex(), "")))

                val announcementObject = Announcement(
                        profileId,
                        announcementId,
                        jAnnouncement.get("Temat").asString,
                        jAnnouncement.get("Tresc").asString,
                        startDate,
                        null,
                        rTeacher.id,
                        null
                )
                data.announcementList.add(announcementObject)
                data.metadataList.add(Metadata(
                        profileId,
                        Metadata.TYPE_ANNOUNCEMENT,
                        announcementObject.id,
                        profile?.empty ?: false,
                        profile?.empty ?: false,
                        addedDate
                ))
            }

            data.setSyncNext(ENDPOINT_IDZIENNIK_WEB_ANNOUNCEMENTS, SYNC_ALWAYS)
            onSuccess()
        }
    }
}
