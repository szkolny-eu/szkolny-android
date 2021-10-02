/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-2.
 */

package pl.szczodrzynski.edziennik.data.api.models

import pl.szczodrzynski.edziennik.data.db.dao.AttendanceDao
import pl.szczodrzynski.edziennik.data.db.dao.EventDao
import pl.szczodrzynski.edziennik.data.db.dao.GradeDao
import pl.szczodrzynski.edziennik.data.db.dao.TimetableDao
import pl.szczodrzynski.edziennik.utils.models.Date

open class DataRemoveModel {
    data class Timetable(private val dateFrom: Date?, private val dateTo: Date?, private val isExtra: Boolean?) : DataRemoveModel() {
        companion object {
            fun from(dateFrom: Date, isExtra: Boolean? = null) = Timetable(dateFrom, null, isExtra)
            fun to(dateTo: Date, isExtra: Boolean? = null) = Timetable(null, dateTo, isExtra)
            fun between(dateFrom: Date, dateTo: Date, isExtra: Boolean? = null) = Timetable(dateFrom, dateTo, isExtra)
        }

        fun commit(profileId: Int, dao: TimetableDao) {
            if (dateFrom != null && dateTo != null) {
                dao.dontKeepBetweenDates(profileId, dateFrom, dateTo, isExtra ?: false)
            } else {
                dateFrom?.let { dateFrom -> dao.dontKeepFromDate(profileId, dateFrom, isExtra ?: false) }
                dateTo?.let { dateTo -> dao.dontKeepToDate(profileId, dateTo, isExtra ?: false) }
            }
        }
    }

    data class Grades(private val all: Boolean, private val semester: Int?, private val type: Int?) : DataRemoveModel() {
        companion object {
            fun all() = Grades(true, null, null)
            fun allWithType(type: Int) = Grades(true, null, type)
            fun semester(semester: Int) = Grades(false, semester, null)
            fun semesterWithType(semester: Int, type: Int) = Grades(false, semester, type)
        }

        fun commit(profileId: Int, dao: GradeDao) {
            if (all) {
                if (type != null) dao.dontKeepWithType(profileId, type)
                else dao.clear(profileId)
            }
            semester?.let {
                if (type != null) dao.dontKeepForSemesterWithType(profileId, it, type)
                else dao.dontKeepForSemester(profileId, it)
            }
        }
    }

    data class Events(private val type: Long?, private val exceptType: Long?, private val exceptTypes: List<Long>?) : DataRemoveModel() {
        companion object {
            fun futureExceptType(exceptType: Long) = Events(null, exceptType, null)
            fun futureExceptTypes(exceptTypes: List<Long>) = Events(null, null, exceptTypes)
            fun futureWithType(type: Long) = Events(type, null, null)
            fun future() = Events(null, null, null)
        }

        fun commit(profileId: Int, dao: EventDao) {
            type?.let { dao.dontKeepFutureWithType(profileId, Date.getToday(), it) }
            exceptType?.let { dao.dontKeepFutureExceptType(profileId, Date.getToday(), it) }
            exceptTypes?.let { dao.dontKeepFutureExceptTypes(profileId, Date.getToday(), it) }
            if (type == null && exceptType == null && exceptTypes == null)
                dao.dontKeepFuture(profileId, Date.getToday())
        }
    }

    data class Attendance(private val dateFrom: Date?) : DataRemoveModel() {
        companion object {
            fun from(dateFrom: Date) = Attendance(dateFrom)
        }

        fun commit(profileId: Int, dao: AttendanceDao) {
            if (dateFrom != null) {
                dao.dontKeepAfterDate(profileId, dateFrom)
            }
        }
    }
}
