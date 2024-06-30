/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-16.
 */

package pl.szczodrzynski.edziennik.data.api.task

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.*
import pl.szczodrzynski.edziennik.data.enums.NotificationType
import pl.szczodrzynski.edziennik.ext.resolveString
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Week

class Notifications(val app: App, val notifications: MutableList<Notification>, val profiles: List<Profile>) {
    companion object {
        private const val TAG = "Notifications"
    }

    private val today by lazy { Date.getToday() }
    private val todayValue by lazy { today.value }

    /**
     * Create a [Notification] from every possible
     * data type. Notifications are posted whenever
     * the object's metadata `notified` property is
     * set to false.
     */
    fun run() {
        timetableNotifications()
        eventNotifications()
        gradeNotifications()
        behaviourNotifications()
        attendanceNotifications()
        announcementNotifications()
        messageNotifications()
        luckyNumberNotifications()
        teacherAbsenceNotifications()
    }

    private fun timetableNotifications() {
        for (lesson in app.db.timetableDao().getNotNotifiedNow().filter { it.displayDate == null || it.displayDate!! >= today }) {
            val text = app.getString(
                    R.string.notification_lesson_change_format,
                    lesson.getDisplayChangeType(app),
                    lesson.displayDate?.formattedString ?: "",
                    lesson.changeSubjectName
            )
            val textLong = app.getString(
                    R.string.notification_lesson_change_long_format,
                    lesson.getDisplayChangeType(app),
                    lesson.displayDate?.formattedString ?: "-",
                    lesson.displayDate?.weekDay?.let { Week.getFullDayName(it) } ?: "-",
                    lesson.changeSubjectName,
                    lesson.changeTeacherName
            )
            notifications += Notification(
                    id = Notification.buildId(lesson.profileId, NotificationType.TIMETABLE_LESSON_CHANGE, lesson.id),
                    title = NotificationType.TIMETABLE_LESSON_CHANGE.titleRes.resolveString(app),
                    text = text,
                    textLong = textLong,
                    type = NotificationType.TIMETABLE_LESSON_CHANGE,
                    profileId = lesson.profileId,
                    profileName = profiles.singleOrNull { it.id == lesson.profileId }?.name,
                    navTarget = NavTarget.TIMETABLE,
                    addedDate = System.currentTimeMillis()
            ).addExtra("timetableDate", lesson.displayDate?.stringY_m_d ?: "")
        }
    }

    private fun eventNotifications() {
        app.db.eventDao().getNotNotifiedNow().filter {
            it.date >= today
        }.forEach { event ->
            val text = if (event.isHomework)
                app.getString(
                    if (event.subjectLongName.isNullOrEmpty())
                        R.string.notification_homework_no_subject_format
                    else
                        R.string.notification_homework_format,
                    event.subjectLongName,
                    event.date.formattedString
                )
            else
                app.getString(
                    if (event.subjectLongName.isNullOrEmpty())
                        R.string.notification_event_no_subject_format
                    else
                        R.string.notification_event_format,
                    event.typeName ?: "wydarzenie",
                    event.date.formattedString,
                    event.subjectLongName
                )
            val textLong = app.getString(
                R.string.notification_event_long_format,
                event.typeName ?: "-",
                event.subjectLongName ?: "-",
                event.date.formattedString,
                Week.getFullDayName(event.date.weekDay),
                event.time?.stringHM ?: app.getString(R.string.event_all_day),
                event.topic.take(200)
            )
            val type = if (event.isHomework)
                NotificationType.HOMEWORK
            else
                NotificationType.EVENT
            notifications += Notification(
                id = Notification.buildId(event.profileId, type, event.id),
                title = type.titleRes.resolveString(app),
                text = text,
                textLong = textLong,
                type = type,
                profileId = event.profileId,
                profileName = profiles.singleOrNull { it.id == event.profileId }?.name,
                navTarget = if (event.isHomework) NavTarget.HOMEWORK else NavTarget.AGENDA,
                addedDate = event.addedDate
            ).addExtra("eventId", event.id).addExtra("eventDate", event.date.value.toLong())
        }
    }

    fun sharedEventNotifications() {
        app.db.eventDao().getNotNotifiedNow().filter {
            it.date >= today && it.sharedBy != null && it.sharedBy != "self"
        }.forEach { event ->
            val text = app.getString(
                R.string.notification_shared_event_format,
                event.sharedByName,
                event.typeName ?: "wydarzenie",
                event.date.formattedString,
                event.topicHtml
            )
            val textLong = app.getString(
                R.string.notification_shared_event_long_format,
                event.sharedByName,
                event.typeName ?: "-",
                event.subjectLongName ?: "-",
                event.date.formattedString,
                Week.getFullDayName(event.date.weekDay),
                event.time?.stringHM ?: app.getString(R.string.event_all_day),
                event.topicHtml.take(200)
            )
            val type = if (event.isHomework)
                NotificationType.HOMEWORK
            else
                NotificationType.EVENT
            notifications += Notification(
                id = Notification.buildId(event.profileId, type, event.id),
                title = type.titleRes.resolveString(app),
                text = text,
                textLong = textLong,
                type = type,
                profileId = event.profileId,
                profileName = profiles.singleOrNull { it.id == event.profileId }?.name,
                navTarget = if (event.isHomework) NavTarget.HOMEWORK else NavTarget.AGENDA,
                addedDate = event.addedDate
            ).addExtra("eventId", event.id).addExtra("eventDate", event.date.value.toLong())
        }
    }

    private fun gradeNotifications() {
        for (grade in app.db.gradeDao().getNotNotifiedNow()) {
            val gradeName = when (grade.type) {
                Grade.TYPE_SEMESTER1_PROPOSED, Grade.TYPE_SEMESTER2_PROPOSED -> app.getString(R.string.grade_semester_proposed_format_2, grade.name)
                Grade.TYPE_SEMESTER1_FINAL, Grade.TYPE_SEMESTER2_FINAL -> app.getString(R.string.grade_semester_final_format_2, grade.name)
                Grade.TYPE_YEAR_PROPOSED -> app.getString(R.string.grade_year_proposed_format_2, grade.name)
                Grade.TYPE_YEAR_FINAL -> app.getString(R.string.grade_year_final_format_2, grade.name)
                else -> grade.name
            }
            val text = app.getString(
                    R.string.notification_grade_format,
                    gradeName,
                    grade.subjectLongName
            )
            val textLong = app.getString(
                    R.string.notification_grade_long_format,
                    gradeName,
                    grade.weight.toString(),
                    grade.subjectLongName ?: "-",
                    grade.category ?: "-",
                    grade.description ?: "-",
                    grade.teacherName ?: "-"
            )
            notifications += Notification(
                    id = Notification.buildId(grade.profileId, NotificationType.GRADE, grade.id),
                    title = NotificationType.GRADE.titleRes.resolveString(app),
                    text = text,
                    textLong = textLong,
                    type = NotificationType.GRADE,
                    profileId = grade.profileId,
                    profileName = profiles.singleOrNull { it.id == grade.profileId }?.name,
                    navTarget = NavTarget.GRADES,
                    addedDate = grade.addedDate
            ).addExtra("gradeId", grade.id).addExtra("gradesSubjectId", grade.subjectId)
        }
    }

    private fun behaviourNotifications() {
        for (notice in app.db.noticeDao().getNotNotifiedNow()) {

            val noticeTypeStr = when (notice.type) {
                Notice.TYPE_POSITIVE -> app.getString(R.string.notification_notice_praise)
                Notice.TYPE_NEGATIVE -> app.getString(R.string.notification_notice_warning)
                else -> app.getString(R.string.notification_notice_new)
            }

            val text = app.getString(
                    R.string.notification_notice_format,
                    noticeTypeStr,
                    notice.teacherName,
                    Date.fromMillis(notice.addedDate).formattedString
            )
            val textLong = app.getString(
                    R.string.notification_notice_long_format,
                    noticeTypeStr,
                    notice.teacherName ?: "-",
                    notice.text.take(200)
            )
            notifications += Notification(
                    id = Notification.buildId(notice.profileId, NotificationType.NOTICE, notice.id),
                    title = NotificationType.NOTICE.titleRes.resolveString(app),
                    text = text,
                    textLong = textLong,
                    type = NotificationType.NOTICE,
                    profileId = notice.profileId,
                    profileName = profiles.singleOrNull { it.id == notice.profileId }?.name,
                    navTarget = NavTarget.BEHAVIOUR,
                    addedDate = notice.addedDate
            ).addExtra("noticeId", notice.id)
        }
    }

    private fun attendanceNotifications() {
        for (attendance in app.db.attendanceDao().getNotNotifiedNow()) {

            val attendanceTypeStr = when (attendance.baseType) {
                Attendance.TYPE_ABSENT -> app.getString(R.string.notification_absence)
                Attendance.TYPE_ABSENT_EXCUSED -> app.getString(R.string.notification_absence_excused)
                Attendance.TYPE_BELATED -> app.getString(R.string.notification_belated)
                Attendance.TYPE_BELATED_EXCUSED -> app.getString(R.string.notification_belated_excused)
                Attendance.TYPE_RELEASED -> app.getString(R.string.notification_release)
                Attendance.TYPE_DAY_FREE -> app.getString(R.string.notification_day_free)
                else -> app.getString(R.string.notification_type_attendance)
            }

            val text = app.getString(
                    if (attendance.subjectLongName.isNullOrEmpty())
                        R.string.notification_attendance_no_lesson_format
                    else
                        R.string.notification_attendance_format,
                    attendanceTypeStr,
                    attendance.subjectLongName,
                    attendance.date.formattedString
            )
            val textLong = app.getString(
                    R.string.notification_attendance_long_format,
                    attendanceTypeStr,
                    attendance.date.formattedString,
                    attendance.startTime?.stringHM ?: "-",
                    attendance.lessonNumber ?: "-",
                    attendance.subjectLongName ?: "-",
                    attendance.teacherName ?: "-",
                    attendance.lessonTopic ?: "-"
            )
            notifications += Notification(
                    id = Notification.buildId(attendance.profileId, NotificationType.ATTENDANCE, attendance.id),
                    title = NotificationType.ATTENDANCE.titleRes.resolveString(app),
                    text = text,
                    textLong = textLong,
                    type = NotificationType.ATTENDANCE,
                    profileId = attendance.profileId,
                    profileName = profiles.singleOrNull { it.id == attendance.profileId }?.name,
                    navTarget = NavTarget.ATTENDANCE,
                    addedDate = attendance.addedDate
            ).addExtra("attendanceId", attendance.id).addExtra("attendanceSubjectId", attendance.subjectId)
        }
    }

    private fun announcementNotifications() {
        for (announcement in app.db.announcementDao().getNotNotifiedNow()) {
            val text = app.getString(
                    R.string.notification_announcement_format,
                    announcement.teacherName,
                    announcement.subject
            )
            notifications += Notification(
                    id = Notification.buildId(announcement.profileId, NotificationType.ANNOUNCEMENT, announcement.id),
                    title = NotificationType.ANNOUNCEMENT.titleRes.resolveString(app),
                    text = text,
                    type = NotificationType.ANNOUNCEMENT,
                    profileId = announcement.profileId,
                    profileName = profiles.singleOrNull { it.id == announcement.profileId }?.name,
                    navTarget = NavTarget.ANNOUNCEMENTS,
                    addedDate = announcement.addedDate
            ).addExtra("announcementId", announcement.id)
        }
    }

    private fun messageNotifications() {
        for (message in app.db.messageDao().getNotNotifiedNow()) {
            val text = app.getString(
                    R.string.notification_message_format,
                    message.senderName,
                    message.subject
            )
            notifications += Notification(
                    id = Notification.buildId(message.profileId, NotificationType.MESSAGE, message.id),
                    title = NotificationType.MESSAGE.titleRes.resolveString(app),
                    text = text,
                    type = NotificationType.MESSAGE,
                    profileId = message.profileId,
                    profileName = profiles.singleOrNull { it.id == message.profileId }?.name,
                    navTarget = NavTarget.MESSAGES,
                    addedDate = message.addedDate
            ).addExtra("messageType", Message.TYPE_RECEIVED.toLong()).addExtra("messageId", message.id)
        }
    }

    private fun luckyNumberNotifications() {
        val luckyNumbers = app.db.luckyNumberDao().getNotNotifiedNow().toMutableList()
        luckyNumbers.removeAll { it.date < today }
        luckyNumbers.forEach { luckyNumber ->
            val profile = profiles.singleOrNull { it.id == luckyNumber.profileId } ?: return@forEach
            val text = when (profile.studentNumber != -1 && profile.studentNumber == luckyNumber.number) {
                true -> when (luckyNumber.date.value) {
                    todayValue -> R.string.notification_lucky_number_yours_format
                    todayValue + 1 -> R.string.notification_lucky_number_yours_tomorrow_format
                    else -> R.string.notification_lucky_number_yours_later_format
                }
                else -> when (luckyNumber.date.value) {
                    todayValue -> R.string.notification_lucky_number_format
                    todayValue + 1 -> R.string.notification_lucky_number_tomorrow_format
                    else -> R.string.notification_lucky_number_later_format
                }
            }
            notifications += Notification(
                    id = Notification.buildId(luckyNumber.profileId, NotificationType.LUCKY_NUMBER, luckyNumber.date.value.toLong()),
                    title = NotificationType.LUCKY_NUMBER.titleRes.resolveString(app),
                    text = app.getString(text, luckyNumber.date.formattedString, luckyNumber.number),
                    type = NotificationType.LUCKY_NUMBER,
                    profileId = luckyNumber.profileId,
                    profileName = profile.name,
                    navTarget = NavTarget.HOME,
                    addedDate = System.currentTimeMillis()
            )
        }
    }

    private fun teacherAbsenceNotifications() {
        for (teacherAbsence in app.db.teacherAbsenceDao().getNotNotifiedNow()) {
            val message = app.getString(
                    R.string.notification_teacher_absence_new_format,
                    teacherAbsence.teacherName
            )
            notifications += Notification(
                    id = Notification.buildId(teacherAbsence.profileId, NotificationType.TEACHER_ABSENCE, teacherAbsence.id),
                    title = NotificationType.TEACHER_ABSENCE.titleRes.resolveString(app),
                    text = message,
                    type = NotificationType.TEACHER_ABSENCE,
                    profileId = teacherAbsence.profileId,
                    profileName = profiles.singleOrNull { it.id == teacherAbsence.profileId }?.name,
                    navTarget = NavTarget.AGENDA,
                    addedDate = teacherAbsence.addedDate
            ).addExtra("eventDate", teacherAbsence.dateFrom.value.toLong())
        }
    }
}
