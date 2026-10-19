package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.html

import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.data.db.entity.Announcement
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.enums.MetadataType
import pl.szczodrzynski.edziennik.utils.models.Date

class MobidziennikHtmlAnnouncements(
    override val data: DataMobidziennik,
    override val lastSync: Long?,
    val onSuccess: (endpointId: Int) -> Unit,
) : MobidziennikWeb(data, lastSync) {
    companion object {
        private const val TAG = "MobidziennikHtmlAnnouncements"
        const val SUBJECT = "Komunikaty szkoły"
    }

    init {
        if (data.profile == null)
            onSuccess(ENDPOINT_MOBIDZIENNIK_HTML_ANNOUNCEMENTS)
        else
            webGet(TAG, "/dziennik/komunikaty") { text ->
                val announcement = HtmlParsers.parseAnnouncements(text)
                if (announcement.isNotEmpty()) {
                    val id = htmlId("announcement", announcement)
                    data.announcementList.add(
                        Announcement(
                            profileId = data.profileId,
                            id = id,
                            subject = SUBJECT,
                            text = announcement,
                            startDate = Date.getToday(),
                            endDate = null,
                            teacherId = -1,
                        )
                    )
                    data.metadataList.add(
                        Metadata(
                            data.profileId,
                            MetadataType.ANNOUNCEMENT,
                            id,
                            data.profile?.empty ?: false,
                            data.profile?.empty ?: false,
                        )
                    )
                }
                data.setSyncNext(ENDPOINT_MOBIDZIENNIK_HTML_ANNOUNCEMENTS, SYNC_ALWAYS)
                onSuccess(ENDPOINT_MOBIDZIENNIK_HTML_ANNOUNCEMENTS)
            }
    }
}
