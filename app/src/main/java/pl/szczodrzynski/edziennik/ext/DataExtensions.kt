/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-17.
 */

package pl.szczodrzynski.edziennik.ext

import android.content.Context
import android.util.LongSparseArray
import androidx.core.util.forEach
import com.google.android.material.datepicker.CalendarConstraints
import com.google.gson.JsonElement
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Notification
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.data.db.entity.Team

fun List<Teacher>.byId(id: Long) = firstOrNull { it.id == id }
fun List<Teacher>.byNameFirstLast(nameFirstLast: String) = firstOrNull { it.name + " " + it.surname == nameFirstLast }
fun List<Teacher>.byNameLastFirst(nameLastFirst: String) = firstOrNull { it.surname + " " + it.name == nameLastFirst }
fun List<Teacher>.byNameFDotLast(nameFDotLast: String) = firstOrNull { it.name + "." + it.surname == nameFDotLast }
fun List<Teacher>.byNameFDotSpaceLast(nameFDotSpaceLast: String) = firstOrNull { it.name + ". " + it.surname == nameFDotSpaceLast }

fun List<Profile>.filterOutArchived() = this.filter { !it.archived }

fun List<Team>.getById(id: Long): Team? {
    return singleOrNull { it.id == id }
}

fun LongSparseArray<Team>.getById(id: Long): Team? {
    forEach { _, value ->
        if (value.id == id)
            return value
    }
    return null
}

operator fun Profile.set(key: String, value: JsonElement) = this.studentData.add(key, value)
operator fun Profile.set(key: String, value: Boolean) = this.studentData.addProperty(key, value)
operator fun Profile.set(key: String, value: String?) = this.studentData.addProperty(key, value)
operator fun Profile.set(key: String, value: Number) = this.studentData.addProperty(key, value)
operator fun Profile.set(key: String, value: Char) = this.studentData.addProperty(key, value)

fun Context.getNotificationTitle(type: Int): String {
    return getString(when (type) {
        Notification.TYPE_UPDATE -> R.string.notification_type_update
        Notification.TYPE_ERROR -> R.string.notification_type_error
        Notification.TYPE_TIMETABLE_CHANGED -> R.string.notification_type_timetable_change
        Notification.TYPE_TIMETABLE_LESSON_CHANGE -> R.string.notification_type_timetable_lesson_change
        Notification.TYPE_NEW_GRADE -> R.string.notification_type_new_grade
        Notification.TYPE_NEW_EVENT -> R.string.notification_type_new_event
        Notification.TYPE_NEW_HOMEWORK -> R.string.notification_type_new_homework
        Notification.TYPE_NEW_SHARED_EVENT -> R.string.notification_type_new_shared_event
        Notification.TYPE_NEW_SHARED_HOMEWORK -> R.string.notification_type_new_shared_homework
        Notification.TYPE_REMOVED_SHARED_EVENT -> R.string.notification_type_removed_shared_event
        Notification.TYPE_NEW_MESSAGE -> R.string.notification_type_new_message
        Notification.TYPE_NEW_NOTICE -> R.string.notification_type_notice
        Notification.TYPE_NEW_ATTENDANCE -> R.string.notification_type_attendance
        Notification.TYPE_SERVER_MESSAGE -> R.string.notification_type_server_message
        Notification.TYPE_LUCKY_NUMBER -> R.string.notification_type_lucky_number
        Notification.TYPE_FEEDBACK_MESSAGE -> R.string.notification_type_feedback_message
        Notification.TYPE_NEW_ANNOUNCEMENT -> R.string.notification_type_new_announcement
        Notification.TYPE_AUTO_ARCHIVING -> R.string.notification_type_auto_archiving
        Notification.TYPE_TEACHER_ABSENCE -> R.string.notification_type_new_teacher_absence
        Notification.TYPE_GENERAL -> R.string.notification_type_general
        else -> R.string.notification_type_general
    })
}

fun Profile.getSchoolYearConstrains(): CalendarConstraints {
    return CalendarConstraints.Builder()
        .setStart(dateSemester1Start.inMillisUtc)
        .setEnd(dateYearEnd.inMillisUtc)
        .build()
}
