package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.html

import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.Notice
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.enums.MetadataType
import pl.szczodrzynski.edziennik.ext.dateToSemester
import pl.szczodrzynski.edziennik.utils.models.Date

class MobidziennikHtmlBehaviour(
    override val data: DataMobidziennik,
    override val lastSync: Long?,
    val onSuccess: (endpointId: Int) -> Unit,
) : MobidziennikWeb(data, lastSync) {
    companion object {
        private const val TAG = "MobidziennikHtmlBehaviour"
    }

    init {
        val profile = data.profile
        if (profile == null)
            onSuccess(ENDPOINT_MOBIDZIENNIK_HTML_BEHAVIOUR)
        else
            webGet(TAG, "/dziennik/zachowanie") { text ->
                for (table in HtmlParsers.parseGenericTables(text)) {
                    for (row in table.drop(1)) {
                        val cells = row.filter { it.isNotEmpty() }
                        if (cells.size < 2) continue
                        val noticeText = cells.maxByOrNull { it.length } ?: continue
                        val millis = row.firstNotNullOfOrNull { data.htmlDateTimeMillis(it) }
                        val semester = millis?.let {
                            profile.dateToSemester(Date.fromMillis(it))
                        } ?: profile.currentSemester
                        val id = htmlId("notice", row.joinToString("|"))
                        data.noticeList.add(
                            Notice(
                                profileId = data.profileId,
                                id = id,
                                type = Notice.TYPE_NEUTRAL,
                                semester = semester,
                                text = noticeText,
                                category = cells.first().takeIf { it != noticeText },
                                points = null,
                                teacherId = -1,
                                addedDate = millis ?: System.currentTimeMillis(),
                            )
                        )
                        data.metadataList.add(
                            Metadata(data.profileId, MetadataType.NOTICE, id, profile.empty, profile.empty)
                        )
                    }
                }
                data.setSyncNext(ENDPOINT_MOBIDZIENNIK_HTML_BEHAVIOUR, SYNC_ALWAYS)
                onSuccess(ENDPOINT_MOBIDZIENNIK_HTML_BEHAVIOUR)
            }
    }
}
