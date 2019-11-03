/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-25.
 */

package pl.szczodrzynski.edziennik.data.db.modules.timetable

import androidx.room.ColumnInfo
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

open class Lesson(val profileId: Int) {
    companion object {
        const val TYPE_NORMAL = 0
        const val TYPE_CANCELLED = 1
        const val TYPE_CHANGE = 2
        const val TYPE_SHIFTED_SOURCE = 3 /* source lesson */
        const val TYPE_SHIFTED_TARGET = 4 /* target lesson */
    }

    @ColumnInfo(name = "lessonType")
    var type: Int = TYPE_NORMAL

    var date: Date? = null
    var lessonNumber: Int? = null
    var startTime: Time? = null
    var endTime: Time? = null
    var teacherId: Long? = null
    var subjectId: Long? = null
    var teamId: Long? = null
    var classroom: String? = null

    var oldDate: Date? = null
    var oldLessonNumber: Int? = null
    var oldStartTime: Time? = null
    var oldEndTime: Time? = null
    var oldTeacherId: Long? = null
    var oldSubjectId: Long? = null
    var oldTeamId: Long? = null
    var oldClassroom: String? = null
}