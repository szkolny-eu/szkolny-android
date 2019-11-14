/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-10.
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data.api

import androidx.core.util.isEmpty
import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.api.v2.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.librus.ENDPOINT_LIBRUS_API_TIMETABLES
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.api.v2.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.data.db.modules.timetable.Lesson
import pl.szczodrzynski.edziennik.utils.Utils.d
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

class LibrusApiTimetables(override val data: DataLibrus,
                        val onSuccess: () -> Unit) : LibrusApi(data) {
    companion object {
        const val TAG = "LibrusApiTimetables"
    }

    init {
        if (data.classrooms.isEmpty()) {
            data.db.classroomDao().getAllNow(profileId).toSparseArray(data.classrooms) { it.id }
        }

        val currentWeekStart = Date.getToday().let { it.stepForward(0, 0, -it.weekDay) }
        val getDate = data.arguments?.getString("weekStart") ?: currentWeekStart.stringY_m_d
        apiGet(TAG, "Timetables?weekStart=$getDate") { json ->
            val days = json.getJsonObject("Timetable")

            days?.entrySet()?.forEach { (dateString, dayEl) ->
                val day = dayEl?.asJsonArray

                val lessonDate = dateString?.let { Date.fromY_m_d(it) } ?: return@forEach

                var lessonsFound = false
                day?.forEach { lessonRangeEl ->
                    val lessonRange = lessonRangeEl?.asJsonArray?.asJsonObjectList()
                    if (lessonRange?.isNullOrEmpty() == false)
                        lessonsFound = true
                    lessonRange?.forEach { lesson ->
                        parseLesson(lessonDate, lesson)
                    }
                }

                if (day.isNullOrEmpty() || !lessonsFound) {
                    data.lessonNewList.add(Lesson(profileId, lessonDate.value.toLong()).apply {
                        type = Lesson.TYPE_NO_LESSONS
                        date = lessonDate
                    })
                }
            }

            val weekStart = Date.fromY_m_d(getDate)
            val weekEnd = weekStart.clone().stepForward(0, 0, 6)
            d(TAG, "Clearing lessons between ${weekStart.stringY_m_d} and ${weekEnd.stringY_m_d} - timetable downloaded for $getDate")

            data.toRemove.add(DataRemoveModel.Timetable.between(weekStart, weekEnd))
            data.setSyncNext(ENDPOINT_LIBRUS_API_TIMETABLES, SYNC_ALWAYS)
            onSuccess()
        }
    }

    private fun parseLesson(lessonDate: Date, lesson: JsonObject) {
        val isSubstitution = lesson.getBoolean("IsSubstitutionClass") ?: false
        val isCancelled = lesson.getBoolean("IsCanceled") ?: false

        val lessonNo = lesson.getInt("LessonNo") ?: return
        val startTime = lesson.getString("HourFrom")?.let { Time.fromH_m(it) } ?: return
        val endTime = lesson.getString("HourTo")?.let { Time.fromH_m(it) } ?: return
        val subjectId = lesson.getJsonObject("Subject")?.getLong("Id")
        val teacherId = lesson.getJsonObject("Teacher")?.getLong("Id")
        val classroomId = lesson.getJsonObject("Classroom")?.getLong("Id") ?: -1
        val virtualClassId = lesson.getJsonObject("VirtualClass")?.getLong("Id")
        val teamId = lesson.getJsonObject("Class")?.getLong("Id") ?: virtualClassId

        val id = lessonDate.combineWith(startTime) / 6L * 10L + (lesson.hashCode() and 0xFFFF)
        val lessonObject = Lesson(profileId, id)

        if (isSubstitution && isCancelled) {
            // shifted lesson - source
            val newDate = lesson.getString("NewDate")?.let { Date.fromY_m_d(it) } ?: return
            val newLessonNo = lesson.getInt("NewLessonNo") ?: return
            val newStartTime = lesson.getString("NewHourFrom")?.let { Time.fromH_m(it) } ?: return
            val newEndTime = lesson.getString("NewHourTo")?.let { Time.fromH_m(it) } ?: return
            val newSubjectId = lesson.getJsonObject("NewSubject")?.getLong("Id")
            val newTeacherId = lesson.getJsonObject("NewTeacher")?.getLong("Id")
            val newClassroomId = lesson.getJsonObject("NewClassroom")?.getLong("Id") ?: -1
            val newVirtualClassId = lesson.getJsonObject("NewVirtualClass")?.getLong("Id")
            val newTeamId = lesson.getJsonObject("NewClass")?.getLong("Id") ?: newVirtualClassId

            lessonObject.let {
                it.type = Lesson.TYPE_SHIFTED_SOURCE
                it.oldDate = lessonDate
                it.oldLessonNumber = lessonNo
                it.oldStartTime = startTime
                it.oldEndTime = endTime
                it.oldSubjectId = subjectId
                it.oldTeacherId = teacherId
                it.oldTeamId = teamId
                it.oldClassroom = data.classrooms[classroomId]?.name

                it.date = newDate
                it.lessonNumber = newLessonNo
                it.startTime = newStartTime
                it.endTime = newEndTime
                it.subjectId = newSubjectId
                it.teacherId = newTeacherId
                it.teamId = newTeamId
                it.classroom = data.classrooms[newClassroomId]?.name
            }
        }
        else if (isSubstitution) {
            // lesson change OR shifted lesson - target
            val oldDate = lesson.getString("OrgDate")?.let { Date.fromY_m_d(it) } ?: return
            val oldLessonNo = lesson.getInt("OrgLessonNo") ?: return
            val oldStartTime = lesson.getString("OrgHourFrom")?.let { Time.fromH_m(it) } ?: return
            val oldEndTime = lesson.getString("OrgHourTo")?.let { Time.fromH_m(it) } ?: return
            val oldSubjectId = lesson.getJsonObject("OrgSubject")?.getLong("Id")
            val oldTeacherId = lesson.getJsonObject("OrgTeacher")?.getLong("Id")
            val oldClassroomId = lesson.getJsonObject("OrgClassroom")?.getLong("Id") ?: -1
            val oldVirtualClassId = lesson.getJsonObject("OrgVirtualClass")?.getLong("Id")
            val oldTeamId = lesson.getJsonObject("OrgClass")?.getLong("Id") ?: oldVirtualClassId

            lessonObject.let {
                it.type = if (lessonDate == oldDate && lessonNo == oldLessonNo) Lesson.TYPE_CHANGE else Lesson.TYPE_SHIFTED_TARGET
                it.oldDate = oldDate
                it.oldLessonNumber = oldLessonNo
                it.oldStartTime = oldStartTime
                it.oldEndTime = oldEndTime
                it.oldSubjectId = oldSubjectId
                it.oldTeacherId = oldTeacherId
                it.oldTeamId = oldTeamId
                it.oldClassroom = data.classrooms[oldClassroomId]?.name

                it.date = lessonDate
                it.lessonNumber = lessonNo
                it.startTime = startTime
                it.endTime = endTime
                it.subjectId = subjectId
                it.teacherId = teacherId
                it.teamId = teamId
                it.classroom = data.classrooms[classroomId]?.name
            }
        }
        else if (isCancelled) {
            lessonObject.let {
                it.type = Lesson.TYPE_CANCELLED
                it.oldDate = lessonDate
                it.oldLessonNumber = lessonNo
                it.oldStartTime = startTime
                it.oldEndTime = endTime
                it.oldSubjectId = subjectId
                it.oldTeacherId = teacherId
                it.oldTeamId = teamId
                it.oldClassroom = data.classrooms[classroomId]?.name
            }
        }
        else {
            lessonObject.let {
                it.type = Lesson.TYPE_NORMAL
                it.date = lessonDate
                it.lessonNumber = lessonNo
                it.startTime = startTime
                it.endTime = endTime
                it.subjectId = subjectId
                it.teacherId = teacherId
                it.teamId = teamId
                it.classroom = data.classrooms[classroomId]?.name
            }
        }

        if (lessonObject.type != Lesson.TYPE_NORMAL) {
            data.metadataList.add(
                    Metadata(
                            data.profileId,
                            Metadata.TYPE_LESSON_CHANGE,
                            lessonObject.id,
                            data.profile?.empty ?: false,
                            data.profile?.empty ?: false,
                            System.currentTimeMillis()
                    ))
        }
        data.lessonNewList.add(lessonObject)
    }
}