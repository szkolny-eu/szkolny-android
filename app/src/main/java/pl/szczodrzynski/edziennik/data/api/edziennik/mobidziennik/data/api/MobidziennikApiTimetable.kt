/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-11.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.api

import android.util.SparseArray
import androidx.core.util.set
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.LessonRange
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.Lesson
import pl.szczodrzynski.edziennik.fixName
import pl.szczodrzynski.edziennik.keys
import pl.szczodrzynski.edziennik.singleOrNull
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

class MobidziennikApiTimetable(val data: DataMobidziennik, rows: List<String>) {
    init { data.profile?.also { profile ->
        val lessons = rows.filterNot { it.isEmpty() }.map { it.split("|") }

        val dataStart = Date.getToday()
        val dataEnd = dataStart.clone().stepForward(0, 0, 7 + (6 - dataStart.weekDay))

        data.toRemove.add(DataRemoveModel.Timetable.between(dataStart.clone(), dataEnd))

        val dataDays = mutableListOf<Int>()
        while (dataStart <= dataEnd) {
            dataDays += dataStart.value
            dataStart.stepForward(0, 0, 1)
        }

        val lessonRanges = SparseArray<Time>()

        for (lesson in lessons) {
            val date = Date.fromYmd(lesson[2])
            val startTime = Time.fromYmdHm(lesson[3])
            val endTime = Time.fromYmdHm(lesson[4])

            val startTimeValue = startTime.value
            lessonRanges[startTimeValue] = endTime

            dataDays.remove(date.value)

            val subjectId = data.subjectList.singleOrNull { it.longName == lesson[5] }?.id ?: -1
            val teacherId = data.teacherList.singleOrNull { it.fullNameLastFirst == (lesson[7]+" "+lesson[6]).fixName() }?.id ?: -1
            val teamId = data.teamList.singleOrNull { it.name == lesson[8]+lesson[9] }?.id ?: -1
            val classroom = lesson[11]

            Lesson(data.profileId, -1).also {
                when (lesson[1]) {
                    "plan_lekcji", "lekcja" -> {
                        it.type = Lesson.TYPE_NORMAL
                        it.date = date
                        it.lessonNumber = startTimeValue
                        it.startTime = startTime
                        it.endTime = endTime
                        it.subjectId = subjectId
                        it.teacherId = teacherId
                        it.teamId = teamId
                        it.classroom = classroom
                    }
                    "lekcja_odwolana" -> {
                        it.type = Lesson.TYPE_CANCELLED
                        it.oldDate = date
                        it.oldLessonNumber = startTimeValue
                        it.oldStartTime = startTime
                        it.oldEndTime = endTime
                        it.oldSubjectId = subjectId
                        //it.oldTeacherId = teacherId
                        it.oldTeamId = teamId
                        //it.oldClassroom = classroom
                    }
                    "zastepstwo" -> {
                        it.type = Lesson.TYPE_CHANGE
                        it.date = date
                        it.lessonNumber = startTimeValue
                        it.startTime = startTime
                        it.endTime = endTime
                        it.subjectId = subjectId
                        it.teacherId = teacherId
                        it.teamId = teamId
                        it.classroom = classroom
                    }
                }

                it.id = it.buildId()

                val seen = profile.empty || date < Date.getToday()

                if (it.type != Lesson.TYPE_NORMAL) {
                    data.metadataList.add(
                            Metadata(
                                    data.profileId,
                                    Metadata.TYPE_LESSON_CHANGE,
                                    it.id,
                                    seen,
                                    seen,
                                    System.currentTimeMillis()
                            ))
                }
                data.lessonList += it
            }
        }

        for (day in dataDays) {
            val lessonDate = Date.fromValue(day)
            data.lessonList += Lesson(data.profileId, lessonDate.value.toLong()).apply {
                type = Lesson.TYPE_NO_LESSONS
                date = lessonDate
            }
        }

        lessonRanges.keys().sorted().forEachIndexed { index, startTime ->
            data.lessonList.forEach {
                if (it.lessonNumber == startTime)
                    it.lessonNumber = index + 1
                if (it.oldLessonNumber == startTime)
                    it.oldLessonNumber = index + 1
            }
            data.lessonRanges[index + 1] = LessonRange(
                    profileId = profile.id,
                    lessonNumber = index + 1,
                    startTime = Time.fromValue(startTime),
                    endTime = lessonRanges[startTime]
            )
        }
    }}
}
