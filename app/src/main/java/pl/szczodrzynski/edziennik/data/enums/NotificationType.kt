/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-20.
 */

package pl.szczodrzynski.edziennik.data.enums

import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import pl.szczodrzynski.edziennik.R

enum class NotificationType(
    val id: Int,
    val icon: IIcon = CommunityMaterial.Icon.cmd_bell_ring_outline,
    val titleRes: Int,
    val pluralRes: Int = R.plurals.notification_other_format,
    val enabledByDefault: Boolean? = true,
) {
    TIMETABLE_LESSON_CHANGE(
        id = 4,
        icon = CommunityMaterial.Icon3.cmd_timetable,
        titleRes = R.string.notification_type_timetable_lesson_change,
        pluralRes = R.plurals.notification_new_timetable_change_format,
    ),
    GRADE(
        id = 5,
        icon = CommunityMaterial.Icon3.cmd_numeric_5_box_outline,
        titleRes = R.string.notification_type_new_grade,
        pluralRes = R.plurals.notification_new_grades_format,
    ),
    EVENT(
        id = 6,
        icon = CommunityMaterial.Icon.cmd_calendar_outline,
        titleRes = R.string.notification_type_new_event,
        pluralRes = R.plurals.notification_new_events_format,
    ),
    HOMEWORK(
        id = 10,
        icon = CommunityMaterial.Icon3.cmd_notebook_outline,
        titleRes = R.string.notification_type_new_homework,
        pluralRes = R.plurals.notification_new_homework_format,
    ),
    MESSAGE(
        id = 8,
        icon = CommunityMaterial.Icon.cmd_email_outline,
        titleRes = R.string.notification_type_new_message,
        pluralRes = R.plurals.notification_new_messages_format,
    ),
    LUCKY_NUMBER(
        id = 14,
        icon = CommunityMaterial.Icon.cmd_emoticon_excited_outline,
        titleRes = R.string.notification_type_lucky_number,
        pluralRes = R.plurals.notification_new_lucky_number_format,
    ),
    NOTICE(
        id = 9,
        icon = CommunityMaterial.Icon.cmd_emoticon_outline,
        titleRes = R.string.notification_type_notice,
        pluralRes = R.plurals.notification_new_notices_format,
    ),
    ATTENDANCE(
        id = 13,
        icon = CommunityMaterial.Icon.cmd_calendar_remove_outline,
        titleRes = R.string.notification_type_attendance,
        pluralRes = R.plurals.notification_new_attendance_format,
    ),
    ANNOUNCEMENT(
        id = 15,
        icon = CommunityMaterial.Icon.cmd_bullhorn_outline,
        titleRes = R.string.notification_type_feedback_message,
        pluralRes = R.plurals.notification_new_announcements_format,
    ),
    SHARED_EVENT(
        id = 7,
        icon = CommunityMaterial.Icon.cmd_calendar_outline,
        titleRes = R.string.notification_type_new_shared_event,
        pluralRes = R.plurals.notification_new_shared_events_format,
    ),
    SHARED_HOMEWORK(
        id = 12,
        icon = CommunityMaterial.Icon3.cmd_notebook_outline,
        titleRes = R.string.notification_type_new_shared_homework,
        pluralRes = R.plurals.notification_new_shared_homework_format,
    ),
    SHARED_NOTE(
        id = 20,
        icon = CommunityMaterial.Icon3.cmd_playlist_edit,
        titleRes = R.string.notification_type_new_shared_note,
    ),
    REMOVED_SHARED_EVENT(
        id = 18,
        titleRes = R.string.notification_type_removed_shared_event,
    ),
    TEACHER_ABSENCE(
        id = 19,
        titleRes = R.string.notification_type_new_teacher_absence,
        enabledByDefault = false,
    ),

    GENERAL(
        id = 0,
        titleRes = R.string.notification_type_general,
        enabledByDefault = null,
    ),
    UPDATE(
        id = 1,
        titleRes = R.string.notification_type_update,
        enabledByDefault = null,
    ),
    ERROR(
        id = 2,
        titleRes = R.string.notification_type_error,
        enabledByDefault = null,
    ),
    TIMETABLE_CHANGED(
        id = 3,
        titleRes = R.string.notification_type_timetable_change,
        enabledByDefault = null,
    ),
    SERVER_MESSAGE(
        id = 11,
        titleRes = R.string.notification_type_server_message,
        enabledByDefault = null,
    ),
    FEEDBACK_MESSAGE(
        id = 16,
        titleRes = R.string.notification_type_feedback_message,
        enabledByDefault = null,
    ),
    AUTO_ARCHIVING(
        id = 17,
        titleRes = R.string.notification_type_auto_archiving,
        enabledByDefault = null,
    );

    companion object {
        fun getDefaultConfig() = NotificationType.entries
            .filter { it.enabledByDefault == false }
            .toSet()
    }
}
