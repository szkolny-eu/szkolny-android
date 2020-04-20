/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-18.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.web

import pl.szczodrzynski.edziennik.data.api.POST
import pl.szczodrzynski.edziennik.data.api.Regexes
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.ENDPOINT_MOBIDZIENNIK_WEB_ATTENDANCE
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.Attendance
import pl.szczodrzynski.edziennik.data.db.entity.Attendance.*
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.fixName
import pl.szczodrzynski.edziennik.get
import pl.szczodrzynski.edziennik.singleOrNull
import pl.szczodrzynski.edziennik.utils.Utils.d
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import pl.szczodrzynski.edziennik.utils.models.Week

class MobidziennikWebAttendance(override val data: DataMobidziennik,
                                override val lastSync: Long?,
                                val onSuccess: (endpointId: Int) -> Unit
) : MobidziennikWeb(data, lastSync) {
    companion object {
        private const val TAG = "MobidziennikWebAttendance"
    }

    init { data.profile?.let { profile ->
        val lastSync = lastSync?.let { Date.fromMillis(it) } ?: profile.dateSemester1Start
        var weekStart = Week.getWeekStart()
        val syncWeeks = mutableListOf<Date>(weekStart)
        while (weekStart >= lastSync && weekStart > profile.dateSemester1Start) {
            weekStart = weekStart.clone().stepForward(0, 0, -7)
            syncWeeks += weekStart
        }

        //syncWeeks.clear()
        //syncWeeks += Date.fromY_m_d("2019-12-19")

        syncWeeks.minBy { it.value }?.let {
            data.toRemove.add(DataRemoveModel.Attendance.from(it))
        }

        start(syncWeeks)

    } ?: onSuccess(ENDPOINT_MOBIDZIENNIK_WEB_ATTENDANCE) }

    private fun start(syncWeeks: MutableList<Date>) {
        if (syncWeeks.isEmpty()) {
            data.setSyncNext(ENDPOINT_MOBIDZIENNIK_WEB_ATTENDANCE, SYNC_ALWAYS)
            onSuccess(ENDPOINT_MOBIDZIENNIK_WEB_ATTENDANCE)
            return
        }
        sync(syncWeeks.removeAt(0).stringY_m_d) {
            start(syncWeeks)
        }
    }

    private fun sync(weekStart: String, onSuccess: () -> Unit) {
        val requestTime = System.currentTimeMillis()
        webGet(TAG, "/dziennik/frekwencja", method = POST, parameters = listOf(
                "uczen" to data.studentId,
                "data_poniedzialek" to weekStart
        )) { text ->
            MobidziennikLuckyNumberExtractor(data, text)

            val start = System.currentTimeMillis()

            Regexes.MOBIDZIENNIK_ATTENDANCE_TABLE.findAll(text).forEach { tableResult ->
                val table = tableResult[1]
                val lessonDates = mutableListOf<Date>()
                val entries = mutableListOf<String>()
                Regexes.MOBIDZIENNIK_ATTENDANCE_LESSON_COUNT.findAll(table).forEach {
                    val date = Date.fromY_m_d(it[1])
                    for (i in 0 until (it[2].toIntOrNull() ?: 0)) {
                        lessonDates += date
                    }
                }
                Regexes.MOBIDZIENNIK_ATTENDANCE_ENTRIES.findAll(table).mapTo(entries) { it[1] }

                val dateIterator = lessonDates.iterator()
                val entriesIterator = entries.iterator()
                Regexes.MOBIDZIENNIK_ATTENDANCE_RANGE.findAll(table).let { ranges ->
                    val count = ranges.count()
                    // verify the lesson count is the same as dates & entries
                    if (count != lessonDates.count() || count != entries.count())
                        return@forEach
                    ranges.forEach { range ->
                        val lessonDate = dateIterator.next()
                        val entry = entriesIterator.next()
                        if (entry.isBlank())
                            return@forEach
                        val startTime = Time.fromH_m(range[1])
                        val entryIterator = entry.iterator()
                        range[2].split(" / ").mapNotNull { Regexes.MOBIDZIENNIK_ATTENDANCE_LESSON.find(it) }.forEachIndexed { index, lesson ->
                            val topic = lesson[2]
                            if (topic.startsWith("Lekcja odwołana: ") || !entryIterator.hasNext())
                                return@forEachIndexed
                            val subjectName = lesson[1]
                            //val team = lesson[3]
                            val teacherName = lesson[4].fixName()

                            val teacherId = data.teacherList.singleOrNull { it.fullNameLastFirst == teacherName }?.id ?: -1
                            val subjectId = data.subjectList.singleOrNull { it.longName == subjectName }?.id ?: -1

                            val type = when (entryIterator.nextChar()) {
                                '.' -> TYPE_PRESENT
                                '|' -> TYPE_ABSENT
                                '+' -> TYPE_ABSENT_EXCUSED
                                's' -> TYPE_BELATED
                                'z' -> TYPE_RELEASED
                                else -> TYPE_PRESENT
                            }
                            val semester = data.profile?.dateToSemester(lessonDate) ?: 1

                            val id = lessonDate.combineWith(startTime) / 6L * 10L + (lesson[0].hashCode() and 0xFFFF) + index

                            val attendanceObject = Attendance(
                                    data.profileId,
                                    id,
                                    teacherId,
                                    subjectId,
                                    semester,
                                    topic,
                                    lessonDate,
                                    startTime,
                                    type)

                            data.attendanceList.add(attendanceObject)
                            if (type != TYPE_PRESENT) {
                                data.metadataList.add(
                                        Metadata(
                                                data.profileId,
                                                Metadata.TYPE_ATTENDANCE,
                                                id,
                                                data.profile?.empty ?: false,
                                                data.profile?.empty ?: false,
                                                System.currentTimeMillis()
                                        ))
                            }
                        }
                    }
                }
            }

            d(TAG, "Done in ${System.currentTimeMillis()-start} ms (request ${start-requestTime} ms)")

            onSuccess()
        }
    }
}
