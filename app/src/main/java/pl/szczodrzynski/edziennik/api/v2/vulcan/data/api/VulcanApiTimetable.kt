/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-13
 */

package pl.szczodrzynski.edziennik.api.v2.vulcan.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.api.v2.Regexes
import pl.szczodrzynski.edziennik.api.v2.VULCAN_API_ENDPOINT_TIMETABLE
import pl.szczodrzynski.edziennik.api.v2.models.DataRemoveModel
import pl.szczodrzynski.edziennik.api.v2.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.api.v2.vulcan.ENDPOINT_VULCAN_API_TIMETABLE
import pl.szczodrzynski.edziennik.api.v2.vulcan.data.VulcanApi
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.data.db.modules.subjects.Subject
import pl.szczodrzynski.edziennik.data.db.modules.timetable.Lesson
import pl.szczodrzynski.edziennik.utils.Utils.crc16
import pl.szczodrzynski.edziennik.utils.Utils.d
import pl.szczodrzynski.edziennik.utils.models.Date

class VulcanApiTimetable(override val data: DataVulcan, val onSuccess: () -> Unit) : VulcanApi(data) {
    companion object {
        const val TAG = "VulcanApiTimetable"
    }

    init {
        data.profile?.also { profile ->
            val currentWeekStart = Date.getToday().let { it.stepForward(0, 0, -it.weekDay) }
            val getDate = data.arguments?.getString("weekStart") ?: currentWeekStart.stringY_m_d

            val weekStart = Date.fromY_m_d(getDate)
            val weekEnd = weekStart.clone().stepForward(0, 0, 6)

            apiGet(TAG, VULCAN_API_ENDPOINT_TIMETABLE, parameters = mapOf(
                    "DataPoczatkowa" to weekStart.stringY_m_d,
                    "DataKoncowa" to weekEnd.stringY_m_d,
                    "IdUczen" to data.studentId,
                    "IdOddzial" to data.studentClassId,
                    "IdOkresKlasyfikacyjny" to data.studentSemesterId
            )) { json, _ ->
                val dates: MutableSet<Int> = mutableSetOf()
                val lessons: MutableList<Lesson> = mutableListOf()

                json.getJsonArray("Data")?.asJsonObjectList()?.forEach { lesson ->
                    val lessonDate = Date.fromY_m_d(lesson.getString("DzienTekst"))
                    val lessonNumber = lesson.getInt("NumerLekcji")
                    val lessonRange = data.lessonRanges.singleOrNull { it.lessonNumber == lessonNumber }
                    val startTime = lessonRange?.startTime
                    val endTime = lessonRange?.endTime
                    val teacherId = lesson.getLong("IdPracownik")
                    val teamId = data.studentClassId.toLong()
                    val classroom = lesson.getString("Classroom")

                    val oldTeacherId = lesson.getLong("IdPracownikOld")

                    val changeAnnotation = lesson.getString("AdnotacjaOZmianie") ?: ""
                    val type = when {
                        changeAnnotation.startsWith("(przeniesiona z") -> Lesson.TYPE_SHIFTED_TARGET
                        changeAnnotation.startsWith("(przeniesiona na") -> Lesson.TYPE_SHIFTED_SOURCE
                        changeAnnotation.startsWith("(zastępstwo") -> Lesson.TYPE_CHANGE
                        lesson.getBoolean("PrzekreslonaNazwa") == true -> Lesson.TYPE_CANCELLED
                        else -> Lesson.TYPE_NORMAL
                    }

                    val subjectId = lesson.getLong("IdPrzedmiot")?.let {
                        when (it) {
                            0L -> {
                                val subjectName = lesson.getString("PrzedmiotNazwa") ?: ""

                                data.subjectList.singleOrNull { subject -> subject.longName == subjectName }?.id
                                        ?: {
                                            /**
                                             * CREATE A NEW SUBJECT IF IT DOESN'T EXIST
                                             */

                                            val subjectObject = Subject(
                                                    profileId,
                                                    -1 * crc16(subjectName.toByteArray()).toLong(),
                                                    subjectName,
                                                    subjectName
                                            )
                                            data.subjectList.put(subjectObject.id, subjectObject)
                                            subjectObject.id
                                        }.invoke()
                            }
                            else -> it
                        }
                    }

                    val id = lessonDate.combineWith(startTime) / 6L * 10L + (lesson.hashCode() and 0xFFFF)

                    val lessonObject = Lesson(profileId, id).apply {
                        this.type = type

                        when (type) {
                            Lesson.TYPE_NORMAL, Lesson.TYPE_CHANGE, Lesson.TYPE_SHIFTED_TARGET -> {
                                this.date = lessonDate
                                this.lessonNumber = lessonNumber
                                this.startTime = startTime
                                this.endTime = endTime
                                this.subjectId = subjectId
                                this.teacherId = teacherId
                                this.teamId = teamId
                                this.classroom = classroom

                                this.oldTeacherId = oldTeacherId
                            }

                            Lesson.TYPE_CANCELLED, Lesson.TYPE_SHIFTED_SOURCE -> {
                                this.oldDate = lessonDate
                                this.oldLessonNumber = lessonNumber
                                this.oldStartTime = startTime
                                this.oldEndTime = endTime
                                this.oldSubjectId = subjectId
                                this.oldTeacherId = teacherId
                                this.oldTeamId = teamId
                                this.oldClassroom = classroom
                            }
                        }

                        if (type == Lesson.TYPE_SHIFTED_SOURCE || type == Lesson.TYPE_SHIFTED_TARGET) {
                            val shift = Regexes.VULCAN_SHITFT_ANNOTATION.find(changeAnnotation)
                            val oldLessonNumber = shift?.get(2)?.toInt()
                            val oldLessonDate = shift?.get(3)?.let { Date.fromd_m_Y(it) }

                            val oldLessonRange = data.lessonRanges.singleOrNull { it.lessonNumber == oldLessonNumber }
                            val oldStartTime = oldLessonRange?.startTime
                            val oldEndTime = oldLessonRange?.endTime

                            when (type) {
                                Lesson.TYPE_SHIFTED_SOURCE -> {
                                    this.lessonNumber = oldLessonNumber
                                    this.date = oldLessonDate
                                    this.startTime = oldStartTime
                                    this.endTime = oldEndTime
                                }

                                Lesson.TYPE_SHIFTED_TARGET -> {
                                    this.oldLessonNumber = oldLessonNumber
                                    this.oldDate = oldLessonDate
                                    this.oldStartTime = oldStartTime
                                    this.oldEndTime = oldEndTime
                                }
                            }
                        }
                    }

                    if (type != Lesson.TYPE_NORMAL) {
                        data.metadataList.add(Metadata(
                                profileId,
                                Metadata.TYPE_LESSON_CHANGE,
                                id,
                                profile.empty,
                                profile.empty,
                                System.currentTimeMillis()
                        ))
                    }

                    dates.add(lessonDate.value)
                    lessons.add(lessonObject)
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

                data.setSyncNext(ENDPOINT_VULCAN_API_TIMETABLE, SYNC_ALWAYS)
                onSuccess()
            }
        }
    }
}
