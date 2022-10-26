/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-17.
 */

package pl.szczodrzynski.edziennik.data.db.enums

enum class MetadataType(
    val id: Int,
) {
    GRADE(id = 1),
    NOTICE(id = 2),
    ATTENDANCE(id = 3),
    EVENT(id = 4),
    HOMEWORK(id = 5),
    LESSON_CHANGE(id = 6),
    ANNOUNCEMENT(id = 7),
    MESSAGE(id = 8),
    TEACHER_ABSENCE(id = 9),
    LUCKY_NUMBER(id = 10),
}
