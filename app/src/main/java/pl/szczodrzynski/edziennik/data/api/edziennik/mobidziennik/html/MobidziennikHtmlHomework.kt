package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.html

import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.enums.MetadataType

class MobidziennikHtmlHomework(
    override val data: DataMobidziennik,
    override val lastSync: Long?,
    val onSuccess: (endpointId: Int) -> Unit,
) : MobidziennikWeb(data, lastSync) {
    companion object {
        private const val TAG = "MobidziennikHtmlHomework"
    }

    init {
        if (data.profile == null)
            onSuccess(ENDPOINT_MOBIDZIENNIK_HTML_HOMEWORK)
        else
            webGet(TAG, "/dziennik/zadaniadomowe") { text ->
                val homework = HtmlParsers.parseHomework(text)
                homework.firstNotNullOfOrNull { it.uczen?.toIntOrNull() }?.let { data.studentId = it }
                homework.forEach { add(it) }

                data.toRemove.add(DataRemoveModel.Events.futureWithType(Event.TYPE_HOMEWORK))
                data.setSyncNext(ENDPOINT_MOBIDZIENNIK_HTML_HOMEWORK, SYNC_ALWAYS)
                onSuccess(ENDPOINT_MOBIDZIENNIK_HTML_HOMEWORK)
            }
    }

    private fun add(h: HtmlParsers.Homework) {
        val date = data.htmlDate(h.termin, "homework date") ?: return
        val id = h.id.toLongOrNull() ?: htmlId("homework", h.termin, h.przedmiot, h.tresc)
        val event = Event(
            profileId = data.profileId,
            id = id,
            date = date,
            time = htmlTime(HtmlText.parseTimeRange(h.godziny)?.first ?: HtmlText.parseTime(h.godziny)),
            topic = h.tresc,
            color = null,
            type = Event.TYPE_HOMEWORK,
            teacherId = data.htmlTeacher(h.nauczyciel)?.id ?: -1,
            subjectId = data.htmlSubject(h.przedmiot)?.id ?: -1,
            teamId = data.htmlTeamClass().id,
            addedDate = data.htmlDateTimeMillis(h.wpisano) ?: System.currentTimeMillis(),
        )
        event.homeworkBody = if (h.trescUcieta) null else h.tresc
        event.isDownloaded = !h.trescUcieta

        data.eventList.add(event)
        data.metadataList.add(
            Metadata(
                data.profileId,
                MetadataType.HOMEWORK,
                id,
                data.profile?.empty ?: false,
                data.profile?.empty ?: false,
            )
        )
    }
}
