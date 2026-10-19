package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.html

import android.util.SparseArray
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.Lesson
import pl.szczodrzynski.edziennik.data.db.entity.LessonRange
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.enums.MetadataType
import pl.szczodrzynski.edziennik.ext.keys
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import pl.szczodrzynski.edziennik.utils.models.Week

class MobidziennikHtmlTimetable(
    override val data: DataMobidziennik,
    override val lastSync: Long?,
    val onSuccess: (endpointId: Int) -> Unit,
) : MobidziennikWeb(data, lastSync) {
    companion object {
        private const val TAG = "MobidziennikHtmlTimetable"
        private val roomRegex = Regex("\\(([^)]*)\\)")

        fun parseDetails(details: String): Triple<String?, String?, String?> {
            val classroom = roomRegex.find(details)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
            val tokens = details.replace(roomRegex, " ").split("|").map { HtmlParsers.clean(it) }.filter { it.isNotEmpty() }
            val withTeacher = tokens.firstOrNull { " - " in it }
            return if (withTeacher != null)
                Triple(withTeacher.substringBefore(" - ").trim(), withTeacher.substringAfter(" - ").trim(), classroom)
            else
                Triple(null, null, classroom)
        }
    }

    private val weekStart: Date = Week.getWeekStart()
    private val weekEnd: Date = weekStart.clone().stepForward(0, 0, 13)
    private val lessons = mutableListOf<Lesson>()
    private val daysWithLessons = mutableSetOf<Int>()
    private val ranges = SparseArray<Time>()

    init {
        if (data.profile == null)
            onSuccess(ENDPOINT_MOBIDZIENNIK_HTML_TIMETABLE)
        else
            sync(mutableListOf(weekStart.clone(), weekStart.clone().stepForward(0, 0, 7)))
    }

    private fun sync(weeks: MutableList<Date>) {
        if (weeks.isEmpty()) {
            finish()
            return
        }
        val monday = weeks.removeAt(0)
        webGet(TAG, "/dziennik/planlekcji?typ=podstawowy&tydzien=${monday.stringY_m_d}") { text ->
            for (lesson in HtmlParsers.parseTimetable(text).lekcje)
                addLesson(lesson)
            sync(weeks)
        }
    }

    private fun addLesson(l: HtmlParsers.TimetableLesson) {
        val profile = data.profile ?: return
        val date = data.htmlDate(l.data, "timetable day") ?: return
        val (startHms, endHms) = HtmlText.parseTimeRange(l.godziny) ?: return
        val start = htmlTime(startHms) ?: return
        val end = htmlTime(endHms) ?: return
        val subject = data.htmlSubject(l.przedmiot)
        val (_, teacherName, classroom) = parseDetails(l.szczegoly)
        val teacher = data.htmlTeacher(teacherName)
        val team = data.htmlTeamClass()

        val note = "${l.uwaga} ${l.status ?: ""}".lowercase()
        val isRoomChange = "zmiana sali" in l.uwaga.lowercase()
        val type = when {
            "odwo" in note || l.rodzaj == "lo" -> Lesson.TYPE_CANCELLED
            isRoomChange || "zast" in note || note.trim().startsWith("zmiana") || l.rodzaj == "lz" ->
                Lesson.TYPE_CHANGE
            else -> Lesson.TYPE_NORMAL
        }

        val lesson = Lesson(data.profileId, -1).also {
            if (type == Lesson.TYPE_CANCELLED) {
                it.type = type
                it.oldDate = date
                it.oldLessonNumber = start.value
                it.oldStartTime = start
                it.oldEndTime = end
                it.oldSubjectId = subject?.id ?: -1
                it.oldTeamId = team.id
            } else {
                it.type = type
                it.date = date
                it.lessonNumber = start.value
                it.startTime = start
                it.endTime = end
                it.subjectId = subject?.id ?: -1
                it.teacherId = teacher?.id ?: -1
                it.teamId = team.id
                it.classroom = classroom
                if (isRoomChange) {
                    it.oldSubjectId = subject?.id
                    it.oldTeacherId = teacher?.id
                    it.oldTeamId = team.id
                }
            }
            it.id = it.buildId()
            it.ownerId = it.buildOwnerId()
        }
        daysWithLessons += date.value
        ranges[start.value] = end

        if (type != Lesson.TYPE_NORMAL) {
            val seen = profile.empty || date < Date.getToday()
            data.metadataList.add(Metadata(data.profileId, MetadataType.LESSON_CHANGE, lesson.id, seen, seen))
        }
        lessons += lesson
    }

    private fun finish() {
        val profile = data.profile
        if (profile != null) {
            data.toRemove.add(DataRemoveModel.Timetable.between(weekStart.clone(), weekEnd.clone(), isExtra = false))

            val day = weekStart.clone()
            while (day <= weekEnd) {
                if (day.value !in daysWithLessons) {
                    lessons += Lesson(data.profileId, day.value.toLong()).apply {
                        type = Lesson.TYPE_NO_LESSONS
                        date = day.clone()
                    }
                }
                day.stepForward(0, 0, 1)
            }

            ranges.keys().sorted().forEachIndexed { index, startValue ->
                lessons.forEach {
                    if (it.lessonNumber == startValue) it.lessonNumber = index + 1
                    if (it.oldLessonNumber == startValue) it.oldLessonNumber = index + 1
                }
                data.lessonRanges[index + 1] = LessonRange(
                    profileId = profile.id,
                    lessonNumber = index + 1,
                    startTime = Time.fromValue(startValue),
                    endTime = ranges[startValue],
                )
            }
            data.lessonList.addAll(lessons)
        }
        data.setSyncNext(ENDPOINT_MOBIDZIENNIK_HTML_TIMETABLE, SYNC_ALWAYS)
        onSuccess(ENDPOINT_MOBIDZIENNIK_HTML_TIMETABLE)
    }
}
