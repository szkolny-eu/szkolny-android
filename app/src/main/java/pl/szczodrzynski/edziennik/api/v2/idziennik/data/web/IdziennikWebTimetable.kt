/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-27.
 */

package pl.szczodrzynski.edziennik.api.v2.idziennik.data.web

import androidx.core.util.set
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.api.v2.ERROR_IDZIENNIK_WEB_REQUEST_NO_DATA
import pl.szczodrzynski.edziennik.api.v2.IDZIENNIK_WEB_TIMETABLE
import pl.szczodrzynski.edziennik.api.v2.idziennik.DataIdziennik
import pl.szczodrzynski.edziennik.api.v2.idziennik.ENDPOINT_IDZIENNIK_WEB_TIMETABLE
import pl.szczodrzynski.edziennik.api.v2.idziennik.data.IdziennikWeb
import pl.szczodrzynski.edziennik.api.v2.models.ApiError
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.modules.lessons.Lesson
import pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonChange
import pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonChange.TYPE_CANCELLED
import pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonChange.TYPE_CHANGE
import pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonRange
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import pl.szczodrzynski.edziennik.utils.models.Week

class IdziennikWebTimetable(override val data: DataIdziennik,
                        val onSuccess: () -> Unit) : IdziennikWeb(data) {
    companion object {
        private const val TAG = "IdziennikWebTimetable"
    }

    init {
        val weekStart = Week.getWeekStart()
        if (Date.getToday().weekDay > 4) {
            weekStart.stepForward(0, 0, 7)
        }

        webApiGet(TAG, IDZIENNIK_WEB_TIMETABLE, mapOf(
                "idPozDziennika" to data.registerId,
                "pidRokSzkolny" to data.schoolYearId,
                "data" to weekStart.stringY_m_d+"T10:00:00.000Z"
        )) { result ->
            val json = result.getJsonObject("d") ?: run {
                data.error(ApiError(TAG, ERROR_IDZIENNIK_WEB_REQUEST_NO_DATA)
                        .withApiResponse(result))
                return@webApiGet
            }

            json.getJsonArray("GodzinyLekcyjne")?.asJsonObjectList()?.forEach { range ->
                val lessonRange = LessonRange(
                        profileId,
                        range.getInt("LiczbaP") ?: return@forEach,
                        range.getString("Poczatek")?.let { Time.fromH_m(it) } ?: return@forEach,
                        range.getString("Koniec")?.let { Time.fromH_m(it) } ?: return@forEach
                )
                data.lessonRanges[lessonRange.lessonNumber] = lessonRange
            }

            json.getJsonArray("Przedmioty")?.asJsonObjectList()?.forEach { lesson ->
                val subject = data.getSubject(
                        lesson.getString("Nazwa") ?: return@forEach,
                        lesson.getLong("Id"),
                        lesson.getString("Skrot") ?: ""
                )
                val teacher = data.getTeacherByFDotLast(lesson.getString("Nauczyciel") ?: return@forEach)
                val weekDay = lesson.getInt("DzienTygodnia")?.minus(1) ?: return@forEach
                val lessonRange = data.lessonRanges[lesson.getInt("Godzina")?.plus(1) ?: return@forEach]

                val lessonObject = Lesson(
                        profileId,
                        weekDay,
                        lessonRange.startTime,
                        lessonRange.endTime
                ).apply {
                    subjectId = subject.id
                    teacherId = teacher.id
                    teamId = data.teamClass?.id ?: -1
                    classroomName = lesson.getString("NazwaSali") ?: ""
                }

                data.lessonList.add(lessonObject)

                val type = lesson.getInt("TypZastepstwa") ?: -1
                if (type != -1) {
                    // we have a lesson change to process
                    val lessonChangeObject = LessonChange(
                            profileId,
                            weekStart.clone().stepForward(0, 0, weekDay),
                            lessonObject.startTime,
                            lessonObject.endTime
                    )

                    lessonChangeObject.teamId = lessonObject.teamId
                    lessonChangeObject.teacherId = lessonObject.teacherId
                    lessonChangeObject.subjectId = lessonObject.subjectId
                    lessonChangeObject.classroomName = lessonObject.classroomName
                    when (type) {
                        0 -> lessonChangeObject.type = TYPE_CANCELLED
                        1, 2, 3, 4, 5 -> {
                            lessonChangeObject.type = TYPE_CHANGE
                            val newTeacher = lesson.getString("NauZastepujacy")
                            val newSubject = lesson.getString("PrzedmiotZastepujacy")
                            if (newTeacher != null) {
                                lessonChangeObject.teacherId = data.getTeacherByFDotLast(newTeacher).id
                            }
                            if (newSubject != null) {
                                lessonChangeObject.subjectId = data.getSubject(newSubject, null, "").id
                            }
                        }
                    }

                    data.lessonChangeList.add(lessonChangeObject)
                    data.metadataList.add(Metadata(
                            profileId,
                            Metadata.TYPE_LESSON_CHANGE,
                            lessonChangeObject.id,
                            profile?.empty ?: false,
                            profile?.empty ?: false,
                            System.currentTimeMillis()
                    ))
                }
            }

            data.setSyncNext(ENDPOINT_IDZIENNIK_WEB_TIMETABLE, SYNC_ALWAYS)
            onSuccess()
        }
    }
}
