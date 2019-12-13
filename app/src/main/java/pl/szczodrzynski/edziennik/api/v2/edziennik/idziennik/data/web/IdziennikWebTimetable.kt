/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-22
 */

package pl.szczodrzynski.edziennik.api.v2.edziennik.idziennik.data.web

import androidx.core.util.set
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.api.v2.ERROR_IDZIENNIK_WEB_REQUEST_NO_DATA
import pl.szczodrzynski.edziennik.api.v2.IDZIENNIK_WEB_TIMETABLE
import pl.szczodrzynski.edziennik.api.v2.edziennik.idziennik.DataIdziennik
import pl.szczodrzynski.edziennik.api.v2.edziennik.idziennik.ENDPOINT_IDZIENNIK_WEB_TIMETABLE
import pl.szczodrzynski.edziennik.api.v2.edziennik.idziennik.data.IdziennikWeb
import pl.szczodrzynski.edziennik.api.v2.models.ApiError
import pl.szczodrzynski.edziennik.api.v2.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonRange
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.data.db.modules.timetable.Lesson
import pl.szczodrzynski.edziennik.utils.Utils.d
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import pl.szczodrzynski.edziennik.utils.models.Week

class IdziennikWebTimetable(override val data: DataIdziennik,
                            val onSuccess: () -> Unit) : IdziennikWeb(data) {
    companion object {
        private const val TAG = "IdziennikWebTimetable"
    }

    init { data.profile?.also { profile ->
        val currentWeekStart = Week.getWeekStart()

        if (Date.getToday().weekDay > 4) {
            currentWeekStart.stepForward(0, 0, 7)
        }

        val getDate = data.arguments?.getString("weekStart") ?: currentWeekStart.stringY_m_d

        val weekStart = Date.fromY_m_d(getDate)
        val weekEnd = weekStart.clone().stepForward(0, 0, 6)

        webApiGet(TAG, IDZIENNIK_WEB_TIMETABLE, mapOf(
                "idPozDziennika" to data.registerId,
                "pidRokSzkolny" to data.schoolYearId,
                "data" to "${weekStart.stringY_m_d}T10:00:00.000Z"
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

            val dates = mutableSetOf<Int>()
            val lessons = mutableListOf<Lesson>()

            json.getJsonArray("Przedmioty")?.asJsonObjectList()?.forEach { lesson ->
                val subject = data.getSubject(
                        lesson.getString("Nazwa") ?: return@forEach,
                        lesson.getLong("Id"),
                        lesson.getString("Skrot") ?: ""
                )
                val teacher = data.getTeacherByFDotLast(lesson.getString("Nauczyciel")
                        ?: return@forEach)

                val newSubjectName = lesson.getString("PrzedmiotZastepujacy")
                val newSubject = when (newSubjectName.isNullOrBlank()) {
                    true -> null
                    else -> data.getSubject(newSubjectName, null, newSubjectName)
                }

                val newTeacherName = lesson.getString("NauZastepujacy")
                val newTeacher = when (newTeacherName.isNullOrBlank()) {
                    true -> null
                    else -> data.getTeacherByFDotLast(newTeacherName)
                }

                val weekDay = lesson.getInt("DzienTygodnia")?.minus(1) ?: return@forEach
                val lessonRange = data.lessonRanges[lesson.getInt("Godzina")?.plus(1)
                        ?: return@forEach]
                val lessonDate = weekStart.clone().stepForward(0, 0, weekDay)
                val classroom = lesson.getString("NazwaSali")

                val type = lesson.getInt("TypZastepstwa") ?: -1

                val lessonObject = Lesson(profileId, -1)

                when (type) {
                    1, 2, 3, 4, 5 -> {
                        lessonObject.apply {
                            this.type = Lesson.TYPE_CHANGE

                            this.date = lessonDate
                            this.lessonNumber = lessonRange.lessonNumber
                            this.startTime = lessonRange.startTime
                            this.endTime = lessonRange.endTime
                            this.subjectId = newSubject?.id
                            this.teacherId = newTeacher?.id
                            this.teamId = data.teamClass?.id
                            this.classroom = classroom

                            this.oldDate = lessonDate
                            this.oldLessonNumber = lessonRange.lessonNumber
                            this.oldStartTime = lessonRange.startTime
                            this.oldEndTime = lessonRange.endTime
                            this.oldSubjectId = subject.id
                            this.oldTeacherId = teacher.id
                            this.oldTeamId = data.teamClass?.id
                            this.oldClassroom = classroom
                        }
                    }
                    0 -> {
                        lessonObject.apply {
                            this.type = Lesson.TYPE_CANCELLED

                            this.oldDate = lessonDate
                            this.oldLessonNumber = lessonRange.lessonNumber
                            this.oldStartTime = lessonRange.startTime
                            this.oldEndTime = lessonRange.endTime
                            this.oldSubjectId = subject.id
                            this.oldTeacherId = teacher.id
                            this.oldTeamId = data.teamClass?.id
                            this.oldClassroom = classroom
                        }
                    }
                    else -> {
                        lessonObject.apply {
                            this.type = Lesson.TYPE_NORMAL

                            this.date = lessonDate
                            this.lessonNumber = lessonRange.lessonNumber
                            this.startTime = lessonRange.startTime
                            this.endTime = lessonRange.endTime
                            this.subjectId = subject.id
                            this.teacherId = teacher.id
                            this.teamId = data.teamClass?.id
                            this.classroom = classroom
                        }
                    }
                }

                lessonObject.id = lessonObject.buildId()

                dates.add(lessonDate.value)
                lessons.add(lessonObject)

                val seen = profile.empty || lessonDate < Date.getToday()

                if (lessonObject.type != Lesson.TYPE_NORMAL && lessonDate >= Date.getToday()) {
                    data.metadataList.add(Metadata(
                            profileId,
                            Metadata.TYPE_LESSON_CHANGE,
                            lessonObject.id,
                            seen,
                            seen,
                            System.currentTimeMillis()
                    ))
                }
            }

            val date: Date = weekStart.clone()
            while (date <= weekEnd) {
                if (!dates.contains(date.value)) {
                    lessons.add(Lesson(profileId, date.value.toLong()).apply {
                        this.type = Lesson.TYPE_NO_LESSONS
                        this.date = date.clone()
                    })
                }

                date.stepForward(0, 0, 1)
            }

            d(TAG, "Clearing lessons between ${weekStart.stringY_m_d} and ${weekEnd.stringY_m_d} - timetable downloaded for $getDate")

            data.lessonNewList.addAll(lessons)
            data.toRemove.add(DataRemoveModel.Timetable.between(weekStart, weekEnd))

            data.setSyncNext(ENDPOINT_IDZIENNIK_WEB_TIMETABLE, SYNC_ALWAYS)
            onSuccess()
        }
    }}
}
