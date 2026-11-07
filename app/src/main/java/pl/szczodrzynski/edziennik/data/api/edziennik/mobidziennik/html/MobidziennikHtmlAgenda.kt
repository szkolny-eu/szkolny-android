package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.html

import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.enums.MetadataType
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

class MobidziennikHtmlAgenda(
    override val data: DataMobidziennik,
    override val lastSync: Long?,
    val onSuccess: (endpointId: Int) -> Unit,
) : MobidziennikWeb(data, lastSync) {
    companion object {
        private const val TAG = "MobidziennikHtmlAgenda"
        private const val MAX_CLASS_NAME_LENGTH = 12
    }

    init {
        if (data.profile == null)
            onSuccess(ENDPOINT_MOBIDZIENNIK_HTML_AGENDA)
        else
            tests()
    }

    private fun tests() {
        webGet(TAG, "/dziennik/sprawdziany") { text ->
            HtmlParsers.parseTests(text).forEach { addTest(it) }
            classCalendar()
        }
    }

    private fun classCalendar() {
        webGet(TAG, "/dziennik/kalendarzklasowy") { text ->
            HtmlParsers.parseClassCalendar(text).forEach { addCalendarEvent(it) }
            schoolCalendar()
        }
    }

    private fun schoolCalendar() {
        webGet(TAG, "/dziennik/kalendarz") { text ->
            HtmlParsers.parseSchoolCalendar(text).forEach { addSchoolEvent(it) }
            finish()
        }
    }

    private fun add(event: Event) {
        data.eventList.add(event)
        data.metadataList.add(
            Metadata(
                data.profileId,
                MetadataType.EVENT,
                event.id,
                data.profile?.empty ?: false,
                data.profile?.empty ?: false,
            )
        )
    }

    private fun addTest(t: HtmlParsers.Test) {
        val date = data.htmlDate(t.data, "test date") ?: return
        if (t.klasa.length <= MAX_CLASS_NAME_LENGTH)
            data.htmlSetClassName(t.klasa)

        val kind = t.rodzaj.lowercase()
        val type = when {
            "kartk" in kind -> Event.TYPE_SHORT_QUIZ
            "sprawd" in kind -> Event.TYPE_EXAM
            else -> Event.TYPE_DEFAULT
        }
        val id = htmlId("test", date.stringY_m_d, t.przedmiot, t.rodzaj, t.tematyka)
        add(
            Event(
                profileId = data.profileId,
                id = id,
                date = date,
                time = htmlTime(HtmlText.parseTime(t.dzienGodzina)),
                topic = t.tematyka.ifEmpty { t.rodzaj },
                color = null,
                type = type,
                teacherId = data.htmlTeacher(t.nauczyciel)?.id ?: -1,
                subjectId = data.htmlSubject(t.przedmiot)?.id ?: -1,
                teamId = data.htmlTeamClass().id,
                addedDate = data.htmlDateTimeMillis(t.wpisano) ?: System.currentTimeMillis(),
            )
        )
    }

    private fun addCalendarEvent(e: HtmlParsers.CalendarEvent) {
        if (e.typ != "kalendarz")
            return
        if (data.studentId == HtmlPlaceholder.STUDENT_ID)
            e.idRaw.split(";").getOrNull(1)?.toIntOrNull()?.let { data.studentId = it }
        val date = data.htmlDate(e.data, "class calendar event") ?: return
        val type = when (e.kolor?.lowercase()) {
            "#c54449" -> Event.TYPE_SHORT_QUIZ
            "#ab0001" -> Event.TYPE_EXAM
            "#008928" -> Event.TYPE_CLASS_EVENT
            "#b66000" -> Event.TYPE_EXCURSION
            else -> Event.TYPE_INFORMATION
        }
        var topic = e.tytul
        if (e.opis.isNotEmpty() && e.opis != e.tytul)
            topic += "\n" + e.opis
        val id = e.idRaw.split(";").getOrNull(2)?.toLongOrNull() ?: htmlId("calendar", e.idRaw, e.data, e.tytul)
        add(
            Event(
                profileId = data.profileId,
                id = id,
                date = date,
                time = null,
                topic = topic,
                color = null,
                type = type,
                teacherId = -1,
                subjectId = -1,
                teamId = data.htmlTeamClass().id,
            )
        )
    }

    private fun addSchoolEvent(r: HtmlParsers.SchoolCalendarRow) {
        val date = data.htmlDate(r.termin, "school calendar event") ?: return
        var topic = r.nazwa
        if (r.kategoria.isNotEmpty())
            topic += " (${r.kategoria})"
        if (r.opis.isNotEmpty())
            topic += "\n" + r.opis
        add(
            Event(
                profileId = data.profileId,
                id = htmlId("school", r.termin, r.nazwa),
                date = date,
                time = null,
                topic = topic,
                color = null,
                type = Event.TYPE_INFORMATION,
                teacherId = -1,
                subjectId = -1,
                teamId = data.htmlTeamClass().id,
            )
        )
    }

    private fun finish() {
        listOf(
            Event.TYPE_EXAM,
            Event.TYPE_SHORT_QUIZ,
            Event.TYPE_CLASS_EVENT,
            Event.TYPE_EXCURSION,
            Event.TYPE_INFORMATION,
        ).forEach { data.toRemove.add(DataRemoveModel.Events.futureWithType(it)) }
        data.setSyncNext(ENDPOINT_MOBIDZIENNIK_HTML_AGENDA, SYNC_ALWAYS)
        onSuccess(ENDPOINT_MOBIDZIENNIK_HTML_AGENDA)
    }
}
