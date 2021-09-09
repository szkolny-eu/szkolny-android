/*
 * Copyright (c) Kuba Szczodrzyński 2021-2-21.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.hebe

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.VULCAN_HEBE_ENDPOINT_TIMETABLE
import pl.szczodrzynski.edziennik.data.api.VULCAN_HEBE_ENDPOINT_TIMETABLE_CHANGES
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_HEBE_TIMETABLE
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanHebe
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.Lesson
import pl.szczodrzynski.edziennik.data.db.entity.Lesson.Companion.TYPE_CANCELLED
import pl.szczodrzynski.edziennik.data.db.entity.Lesson.Companion.TYPE_CHANGE
import pl.szczodrzynski.edziennik.data.db.entity.Lesson.Companion.TYPE_NORMAL
import pl.szczodrzynski.edziennik.data.db.entity.Lesson.Companion.TYPE_SHIFTED_SOURCE
import pl.szczodrzynski.edziennik.data.db.entity.Lesson.Companion.TYPE_SHIFTED_TARGET
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.utils.Utils.d
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Week

class VulcanHebeTimetable(
    override val data: DataVulcan,
    override val lastSync: Long?,
    val onSuccess: (endpointId: Int) -> Unit
) : VulcanHebe(data, lastSync) {
    companion object {
        const val TAG = "VulcanHebeTimetable"
    }

    private val lessonList = mutableListOf<Lesson>()
    private val lessonDates = mutableSetOf<Int>()

    init {
        val previousWeekStart = Week.getWeekStart().stepForward(0, 0, -7)
        if (Date.getToday().weekDay > 4) {
            previousWeekStart.stepForward(0, 0, 7)
        }

        val dateFrom = data.arguments
            ?.getString("weekStart")
            ?.let { Date.fromY_m_d(it) }
            ?: previousWeekStart
        val dateTo = dateFrom.clone().stepForward(0, 0, 13)

        val lastSync = 0L

        apiGetList(
            TAG,
            VULCAN_HEBE_ENDPOINT_TIMETABLE,
            HebeFilterType.BY_PUPIL,
            dateFrom = dateFrom,
            dateTo = dateTo,
            lastSync = lastSync
        ) { lessons, _ ->
            apiGetList(
                TAG,
                VULCAN_HEBE_ENDPOINT_TIMETABLE_CHANGES,
                HebeFilterType.BY_PUPIL,
                dateFrom = dateFrom,
                dateTo = dateTo,
                lastSync = lastSync
            ) { changes, _ ->
                processData(lessons, changes)

                // cancel lesson changes when caused by a shift
                for (lesson in lessonList) {
                    if (lesson.type != TYPE_SHIFTED_TARGET)
                        continue
                    lessonList.firstOrNull {
                        it.oldDate == lesson.date
                                && it.oldLessonNumber == lesson.lessonNumber
                                && it.type == TYPE_CHANGE
                    }?.let {
                        it.type = TYPE_CANCELLED
                        it.date = null
                        it.lessonNumber = null
                        it.startTime = null
                        it.endTime = null
                        it.subjectId = null
                        it.teacherId = null
                        it.teamId = null
                        it.classroom = null
                    }
                }

                // add TYPE_NO_LESSONS to empty dates
                val date: Date = dateFrom.clone()
                while (date <= dateTo) {
                    if (!lessonDates.contains(date.value)) {
                        lessonList.add(Lesson(profileId, date.value.toLong()).apply {
                            this.type = Lesson.TYPE_NO_LESSONS
                            this.date = date.clone()
                        })
                    }

                    date.stepForward(0, 0, 1)
                }

                d(
                    TAG,
                    "Clearing lessons between ${dateFrom.stringY_m_d} and ${dateTo.stringY_m_d}"
                )

                data.toRemove.add(DataRemoveModel.Timetable.between(dateFrom, dateTo))

                data.lessonList.addAll(lessonList)

                data.setSyncNext(ENDPOINT_VULCAN_HEBE_TIMETABLE, SYNC_ALWAYS)
                onSuccess(ENDPOINT_VULCAN_HEBE_TIMETABLE)
            }
        }
    }

    private fun buildLesson(changes: List<JsonObject>, json: JsonObject): Pair<Lesson, Lesson?>? {
        val lesson = Lesson(profileId, -1)
        var lessonShift: Lesson? = null

        val lessonDate = getDate(json, "Date") ?: return null
        val lessonRange = getLessonRange(json, "TimeSlot")
        val startTime = lessonRange?.startTime
        val endTime = lessonRange?.endTime
        val teacherId = getTeacherId(json, "TeacherPrimary")
        val classroom = json.getJsonObject("Room").getString("Code")
        val subjectId = getSubjectId(json, "Subject")

        val teamId = getTeamId(json, "Distribution")
            ?: getClassId(json, "Clazz")
            ?: data.teamClass?.id
            ?: -1

        val change = json.getJsonObject("Change")
        val changeId = change.getInt("Id")
        val type = when (change.getInt("Type")) {
            1 -> TYPE_CANCELLED
            2 -> TYPE_CHANGE
            3 -> TYPE_SHIFTED_SOURCE
            4 -> TYPE_CANCELLED // TODO: 2021-02-21 add showing cancellation reason
            else -> TYPE_NORMAL
        }

        lesson.type = type
        if (type == TYPE_NORMAL) {
            lesson.date = lessonDate
            lesson.lessonNumber = lessonRange?.lessonNumber
            lesson.startTime = startTime
            lesson.endTime = endTime
            lesson.subjectId = subjectId
            lesson.teacherId = teacherId
            lesson.teamId = teamId
            lesson.classroom = classroom
        } else {
            lesson.oldDate = lessonDate
            lesson.oldLessonNumber = lessonRange?.lessonNumber
            lesson.oldStartTime = startTime
            lesson.oldEndTime = endTime
            lesson.oldSubjectId = subjectId
            lesson.oldTeacherId = teacherId
            lesson.oldTeamId = teamId
            lesson.oldClassroom = classroom
        }

        if (type == TYPE_CHANGE || type == TYPE_SHIFTED_SOURCE) {
            val changeJson = changes.firstOrNull {
                it.getInt("Id") == changeId
            } ?: return lesson to null

            val changeLessonDate = getDate(changeJson, "LessonDate") ?: return lesson to null
            val changeLessonRange = getLessonRange(changeJson, "TimeSlot") ?: lessonRange
            val changeStartTime = changeLessonRange?.startTime
            val changeEndTime = changeLessonRange?.endTime
            val changeTeacherId = getTeacherId(changeJson, "TeacherPrimary") ?: teacherId
            val changeClassroom = changeJson.getJsonObject("Room").getString("Code") ?: classroom
            val changeSubjectId = getSubjectId(changeJson, "Subject") ?: subjectId

            val changeTeamId = getTeamId(json, "Distribution")
                ?: getClassId(json, "Clazz")
                ?: teamId

            if (type != TYPE_CHANGE) {
                /* lesson shifted */
                lessonShift = Lesson(profileId, -1)
                lessonShift.type = TYPE_SHIFTED_TARGET

                // update source lesson with the target lesson date
                lesson.date = changeLessonDate
                lesson.lessonNumber = changeLessonRange?.lessonNumber
                lesson.startTime = changeStartTime
                lesson.endTime = changeEndTime
                // update target lesson with the source lesson date
                lessonShift.oldDate = lessonDate
                lessonShift.oldLessonNumber = lessonRange?.lessonNumber
                lessonShift.oldStartTime = startTime
                lessonShift.oldEndTime = endTime
            }

            (if (type == TYPE_CHANGE) lesson else lessonShift)
                ?.apply {
                    this.date = changeLessonDate
                    this.lessonNumber = changeLessonRange?.lessonNumber
                    this.startTime = changeStartTime
                    this.endTime = changeEndTime
                    this.subjectId = changeSubjectId
                    this.teacherId = changeTeacherId
                    this.teamId = changeTeamId
                    this.classroom = changeClassroom
                }
        }

        return lesson to lessonShift
    }

    private fun processData(lessons: List<JsonObject>, changes: List<JsonObject>) {
        lessons.forEach { lessonJson ->
            if (lessonJson.getBoolean("Visible") != true)
                return@forEach

            val lessonPair = buildLesson(changes, lessonJson) ?: return@forEach
            val (lessonObject, lessonShift) = lessonPair

            when {
                lessonShift != null -> lessonShift
                lessonObject.type != TYPE_NORMAL -> lessonObject
                else -> null
            }?.let { lesson ->
                val lessonDate = lesson.displayDate ?: return@let
                val seen = profile?.empty ?: true || lessonDate < Date.getToday()
                data.metadataList.add(
                    Metadata(
                        profileId,
                        Metadata.TYPE_LESSON_CHANGE,
                        lesson.id,
                        seen,
                        seen
                    )
                )
            }

            lessonObject.id = lessonObject.buildId()
            lessonShift?.id = lessonShift?.buildId() ?: -1

            lessonList.add(lessonObject)
            lessonShift?.let { lessonList.add(it) }

            lessonObject.displayDate?.let { lessonDates.add(it.value) }
            lessonShift?.displayDate?.let { lessonDates.add(it.value) }
        }
    }
}
