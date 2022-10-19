/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-17.
 */

package pl.szczodrzynski.edziennik.data.db.enums

import pl.szczodrzynski.edziennik.R

enum class FeatureType(
    val id: Int,
    val isAlwaysNeeded: Boolean,
    val nameRes: Int? = null,
) {
    TIMETABLE(id = 1, isAlwaysNeeded = false, nameRes = R.string.menu_timetable),
    AGENDA(id = 2, isAlwaysNeeded = false, nameRes = R.string.menu_agenda),
    GRADES(id = 3, isAlwaysNeeded = false, nameRes = R.string.menu_grades),
    HOMEWORK(id = 4, isAlwaysNeeded = false, nameRes = R.string.menu_homework),
    BEHAVIOUR(id = 5, isAlwaysNeeded = false, nameRes = R.string.menu_notices),
    ATTENDANCE(id = 6, isAlwaysNeeded = false, nameRes = R.string.menu_attendance),
    MESSAGES_INBOX(id = 7, isAlwaysNeeded = false, nameRes = R.string.title_messages_inbox_single),
    MESSAGES_SENT(id = 8, isAlwaysNeeded = false, nameRes = R.string.title_messages_sent_single),
    ANNOUNCEMENTS(id = 9, isAlwaysNeeded = false, nameRes = R.string.menu_announcements),

    ALWAYS_NEEDED(id = 100, isAlwaysNeeded = true),
    STUDENT_INFO(id = 101, isAlwaysNeeded = true),
    STUDENT_NUMBER(id = 109, isAlwaysNeeded = true),
    SCHOOL_INFO(id = 102, isAlwaysNeeded = true),
    CLASS_INFO(id = 103, isAlwaysNeeded = true),
    TEAM_INFO(id = 104, isAlwaysNeeded = true),
    LUCKY_NUMBER(id = 105, isAlwaysNeeded = true),
    TEACHERS(id = 106, isAlwaysNeeded = true),
    SUBJECTS(id = 107, isAlwaysNeeded = true),
    CLASSROOMS(id = 108, isAlwaysNeeded = true),
    PUSH_CONFIG(id = 120, isAlwaysNeeded = true),
}
