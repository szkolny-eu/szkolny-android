package pl.szczodrzynski.edziennik.api.v2

import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_AGENDA
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_ANNOUNCEMENTS
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_ATTENDANCE
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_BEHAVIOUR
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_GRADES
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_HOME
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_HOMEWORK
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_MESSAGES
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_TIMETABLE
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.api.v2.models.Data
import pl.szczodrzynski.edziennik.data.db.modules.attendance.Attendance
import pl.szczodrzynski.edziennik.data.db.modules.events.Event
import pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.*
import pl.szczodrzynski.edziennik.data.db.modules.messages.Message
import pl.szczodrzynski.edziennik.data.db.modules.notices.Notice
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.Companion.TYPE_LUCKY_NUMBER
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.Companion.TYPE_NEW_ANNOUNCEMENT
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.Companion.TYPE_NEW_ATTENDANCE
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.Companion.TYPE_NEW_EVENT
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.Companion.TYPE_NEW_GRADE
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.Companion.TYPE_NEW_HOMEWORK
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.Companion.TYPE_NEW_MESSAGE
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.Companion.TYPE_NEW_NOTICE
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.Companion.TYPE_TIMETABLE_LESSON_CHANGE
import pl.szczodrzynski.edziennik.data.db.modules.notification.getNotificationTitle
import pl.szczodrzynski.edziennik.utils.models.Date

class DataNotifications(val data: Data) {
    companion object {
        private const val TAG = "DataNotifications"
    }

    val app = data.app
    val profileId = data.profile?.id ?: -1
    val profileName = data.profile?.name ?: ""
    val profile = data.profile
    val loginStore = data.loginStore

    init { run {
        if (profile == null) {
            return@run
        }

        for (lesson in app.db.timetableDao().getNotNotifiedNow(profileId)) {
            val text = app.getString(R.string.notification_lesson_change_format, lesson.getDisplayChangeType(app), if (lesson.displayDate == null) "" else lesson.displayDate!!.formattedString, lesson.changeSubjectName)
            data.notifications += Notification(
                    title = app.getNotificationTitle(TYPE_TIMETABLE_LESSON_CHANGE),
                    text = text,
                    type = TYPE_TIMETABLE_LESSON_CHANGE,
                    profileId = profileId,
                    profileName = profileName,
                    viewId = DRAWER_ITEM_TIMETABLE,
                    addedDate = lesson.addedDate
            ).addExtra("timetableDate", lesson.displayDate?.value?.toLong())
        }

        for (event in app.db.eventDao().getNotNotifiedNow(profileId)) {
            val text = if (event.type == Event.TYPE_HOMEWORK)
                app.getString(
                        if (event.subjectLongName.isNullOrEmpty())
                            R.string.notification_homework_no_subject_format
                        else
                            R.string.notification_homework_format,
                        event.subjectLongName,
                        event.eventDate.formattedString
                )
            else
                app.getString(
                        if (event.subjectLongName.isNullOrEmpty())
                            R.string.notification_event_no_subject_format
                        else
                            R.string.notification_event_format,
                        event.typeName,
                        event.eventDate.formattedString,
                        event.subjectLongName
                )
            val type = if (event.type == Event.TYPE_HOMEWORK) TYPE_NEW_HOMEWORK else TYPE_NEW_EVENT
            data.notifications += Notification(
                    title = app.getNotificationTitle(type),
                    text = text,
                    type = type,
                    profileId = profileId,
                    profileName = profileName,
                    viewId = if (event.type == Event.TYPE_HOMEWORK) DRAWER_ITEM_HOMEWORK else DRAWER_ITEM_AGENDA,
                    addedDate = event.addedDate
            ).addExtra("eventId", event.id).addExtra("eventDate", event.eventDate.value.toLong())
        }

        val today = Date.getToday()
        val todayValue = today.value
        profile.currentSemester = profile.dateToSemester(today)

        for (grade in app.db.gradeDao().getNotNotifiedNow(profileId)) {
            val gradeName = when (grade.type) {
                TYPE_SEMESTER1_PROPOSED, TYPE_SEMESTER2_PROPOSED -> app.getString(R.string.grade_semester_proposed_format_2, grade.name)
                TYPE_SEMESTER1_FINAL, TYPE_SEMESTER2_FINAL -> app.getString(R.string.grade_semester_final_format_2, grade.name)
                TYPE_YEAR_PROPOSED -> app.getString(R.string.grade_year_proposed_format_2, grade.name)
                TYPE_YEAR_FINAL -> app.getString(R.string.grade_year_final_format_2, grade.name)
                else -> grade.name
            }
            val text = app.getString(R.string.notification_grade_format, gradeName, grade.subjectLongName)
            data.notifications += Notification(
                    title = app.getNotificationTitle(TYPE_NEW_GRADE),
                    text = text,
                    type = TYPE_NEW_GRADE,
                    profileId = profileId,
                    profileName = profileName,
                    viewId = DRAWER_ITEM_GRADES,
                    addedDate = grade.addedDate
            ).addExtra("gradeId", grade.id).addExtra("gradesSubjectId", grade.subjectId)
        }

        for (notice in app.db.noticeDao().getNotNotifiedNow(profileId)) {
            val noticeTypeStr = if (notice.type == Notice.TYPE_POSITIVE) app.getString(R.string.notification_notice_praise) else if (notice.type == Notice.TYPE_NEGATIVE) app.getString(R.string.notification_notice_warning) else app.getString(R.string.notification_notice_new)
            val text = app.getString(R.string.notification_notice_format, noticeTypeStr, notice.teacherFullName, Date.fromMillis(notice.addedDate).formattedString)
            data.notifications += Notification(
                    title = app.getNotificationTitle(TYPE_NEW_NOTICE),
                    text = text,
                    type = TYPE_NEW_NOTICE,
                    profileId = profileId,
                    profileName = profileName,
                    viewId = DRAWER_ITEM_BEHAVIOUR,
                    addedDate = notice.addedDate
            ).addExtra("noticeId", notice.id)
        }

        for (attendance in app.db.attendanceDao().getNotNotifiedNow(profileId)) {
            var attendanceTypeStr = app.getString(R.string.notification_type_attendance)
            when (attendance.type) {
                Attendance.TYPE_ABSENT -> attendanceTypeStr = app.getString(R.string.notification_absence)
                Attendance.TYPE_ABSENT_EXCUSED -> attendanceTypeStr = app.getString(R.string.notification_absence_excused)
                Attendance.TYPE_BELATED -> attendanceTypeStr = app.getString(R.string.notification_belated)
                Attendance.TYPE_BELATED_EXCUSED -> attendanceTypeStr = app.getString(R.string.notification_belated_excused)
                Attendance.TYPE_RELEASED -> attendanceTypeStr = app.getString(R.string.notification_release)
            }
            val text = app.getString(
                    if (attendance.subjectLongName.isNullOrEmpty())
                        R.string.notification_attendance_no_lesson_format
                    else
                        R.string.notification_attendance_format,
                    attendanceTypeStr,
                    attendance.subjectLongName,
                    attendance.lessonDate.formattedString
            )
            data.notifications += Notification(
                    title = app.getNotificationTitle(TYPE_NEW_ATTENDANCE),
                    text = text,
                    type = TYPE_NEW_ATTENDANCE,
                    profileId = profileId,
                    profileName = profileName,
                    viewId = DRAWER_ITEM_ATTENDANCE,
                    addedDate = attendance.addedDate
            ).addExtra("attendanceId", attendance.id).addExtra("attendanceSubjectId", attendance.subjectId)
        }

        for (announcement in app.db.announcementDao().getNotNotifiedNow(profileId)) {
            val text = app.context.getString(R.string.notification_announcement_format, announcement.subject)
            data.notifications += Notification(
                    title = app.getNotificationTitle(TYPE_NEW_ANNOUNCEMENT),
                    text = text,
                    type = TYPE_NEW_ANNOUNCEMENT,
                    profileId = profileId,
                    profileName = profileName,
                    viewId = DRAWER_ITEM_ANNOUNCEMENTS,
                    addedDate = announcement.addedDate
            ).addExtra("announcementId", announcement.id)
        }

        for (message in app.db.messageDao().getReceivedNotNotifiedNow(profileId)) {
            val text = app.context.getString(R.string.notification_message_format, message.senderFullName, message.subject)
            data.notifications += Notification(
                    title = app.getNotificationTitle(TYPE_NEW_MESSAGE),
                    text = text,
                    type = TYPE_NEW_MESSAGE,
                    profileId = profileId,
                    profileName = profileName,
                    viewId = DRAWER_ITEM_MESSAGES,
                    addedDate = message.addedDate
            ).addExtra("messageType", Message.TYPE_RECEIVED.toLong()).addExtra("messageId", message.id)
        }

        val luckyNumbers = app.db.luckyNumberDao().getNotNotifiedNow(profileId)
        luckyNumbers?.removeAll { it.date < today }
        luckyNumbers?.forEach { luckyNumber ->
            val text = when (luckyNumber.date.value) {
                todayValue -> // LN for today
                    app.getString(if (profile.studentNumber != -1 && profile.studentNumber == luckyNumber.number) R.string.notification_lucky_number_yours_format else R.string.notification_lucky_number_format, luckyNumber.number)
                todayValue + 1 -> // LN for tomorrow
                    app.getString(if (profile.studentNumber != -1 && profile.studentNumber == luckyNumber.number) R.string.notification_lucky_number_yours_tomorrow_format else R.string.notification_lucky_number_tomorrow_format, luckyNumber.number)
                else -> // LN for later
                    app.getString(if (profile.studentNumber != -1 && profile.studentNumber == luckyNumber.number) R.string.notification_lucky_number_yours_later_format else R.string.notification_lucky_number_later_format, luckyNumber.date.formattedString, luckyNumber.number)
            }
            data.notifications += Notification(
                    title = app.getNotificationTitle(TYPE_LUCKY_NUMBER),
                    text = text,
                    type = TYPE_LUCKY_NUMBER,
                    profileId = profileId,
                    profileName = profileName,
                    viewId = DRAWER_ITEM_HOME,
                    addedDate = luckyNumber.addedDate
            )
        }

        data.db.metadataDao().setAllNotified(profileId, true)
    }}
}
