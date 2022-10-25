/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-11.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.api

import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.db.entity.Attendance
import pl.szczodrzynski.edziennik.data.db.entity.Attendance.Companion.TYPE_ABSENT
import pl.szczodrzynski.edziennik.data.db.entity.Attendance.Companion.TYPE_ABSENT_EXCUSED
import pl.szczodrzynski.edziennik.data.db.entity.Attendance.Companion.TYPE_PRESENT
import pl.szczodrzynski.edziennik.data.db.entity.Attendance.Companion.TYPE_RELEASED
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.enums.MetadataType
import pl.szczodrzynski.edziennik.ext.dateToSemester

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
                val baseType = when (cols[4]) {
                    "2" -> TYPE_ABSENT
                    "5" -> TYPE_ABSENT_EXCUSED
                    "4" -> TYPE_RELEASED
                    else -> TYPE_PRESENT
                }
                val semester = data.profile?.dateToSemester(lesson.date) ?: 1

                val typeName = when (baseType) {
                    TYPE_ABSENT -> "nieobecność"
                    TYPE_ABSENT_EXCUSED -> "nieobecność usprawiedliwiona"
                    TYPE_RELEASED -> "zwolnienie"
                    TYPE_PRESENT -> "obecność"
                    else -> "nieznany rodzaj"
                }
                val typeSymbol = when (baseType) {
                    TYPE_ABSENT -> "|"
                    TYPE_ABSENT_EXCUSED -> "+"
                    TYPE_RELEASED -> "z"
                    TYPE_PRESENT -> "."
                    else -> "?"
                }

                val attendanceObject = Attendance(
                        profileId = data.profileId,
                        id = id,
                        baseType = baseType,
                        typeName = typeName,
                        typeShort = data.app.attendanceManager.getTypeShort(baseType),
                        typeSymbol = typeSymbol,
                        typeColor = null,
                        date = lesson.date,
                        startTime = lesson.startTime,
                        semester = semester,
                        teacherId = lesson.teacherId,
                        subjectId = lesson.subjectId
                ).also {
                    it.lessonTopic = lesson.topic
                }

                data.attendanceList.add(attendanceObject)
                data.metadataList.add(
                        Metadata(
                                data.profileId,
                                MetadataType.ATTENDANCE,
                                id,
                                data.profile?.empty ?: false || baseType == Attendance.TYPE_PRESENT_CUSTOM || baseType == Attendance.TYPE_UNKNOWN,
                                data.profile?.empty ?: false || baseType == Attendance.TYPE_PRESENT_CUSTOM || baseType == Attendance.TYPE_UNKNOWN
                        ))
            }
        }
    }}
}
