/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-11.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.api

import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.db.entity.Attendance
import pl.szczodrzynski.edziennik.data.db.entity.Attendance.*
import pl.szczodrzynski.edziennik.data.db.entity.Metadata

class MobidziennikApiAttendance(val data: DataMobidziennik, rows: List<String>) {
    init { run {
        for (row in rows) {
            if (row.isEmpty())
                continue
            val cols = row.split("|")

            val studentId = cols[2].toInt()
            if (studentId != data.studentId)
                return@run

            val id = cols[0].toLong()
            val lessonId = cols[1].toLong()
            data.mobiLessons.singleOrNull { it.id == lessonId }?.let { lesson ->
                val type = when (cols[4]) {
                    "2" -> TYPE_ABSENT
                    "5" -> TYPE_ABSENT_EXCUSED
                    "4" -> TYPE_RELEASED
                    else -> TYPE_PRESENT
                }
                val semester = data.profile?.dateToSemester(lesson.date) ?: 1

                val attendanceObject = Attendance(
                        data.profileId,
                        id,
                        lesson.teacherId,
                        lesson.subjectId,
                        semester,
                        lesson.topic,
                        lesson.date,
                        lesson.startTime,
                        type)

                data.attendanceList.add(attendanceObject)
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
    }}
}
