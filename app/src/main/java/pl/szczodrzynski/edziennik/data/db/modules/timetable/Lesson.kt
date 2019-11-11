/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-25.
 */

package pl.szczodrzynski.edziennik.data.db.modules.timetable

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

@Entity(tableName = "timetable",
        indices = [
            Index(value = ["profileId", "type", "date"]),
            Index(value = ["profileId", "type", "oldDate"])
        ])
open class Lesson(val profileId: Int, @PrimaryKey val id: Long) {
    companion object {
        const val TYPE_NO_LESSONS = -1
        const val TYPE_NORMAL = 0
        const val TYPE_CANCELLED = 1
        const val TYPE_CHANGE = 2
        const val TYPE_SHIFTED_SOURCE = 3 /* source lesson */
        const val TYPE_SHIFTED_TARGET = 4 /* target lesson */
    }

    var type: Int = TYPE_NORMAL

    var date: Date? = null
    var lessonNumber: Int? = null
    var startTime: Time? = null
    var endTime: Time? = null
    var subjectId: Long? = null
    var teacherId: Long? = null
    var teamId: Long? = null
    var classroom: String? = null

    var oldDate: Date? = null
    var oldLessonNumber: Int? = null
    var oldStartTime: Time? = null
    var oldEndTime: Time? = null
    var oldSubjectId: Long? = null
    var oldTeacherId: Long? = null
    var oldTeamId: Long? = null
    var oldClassroom: String? = null

    override fun toString(): String {
        return "Lesson(profileId=$profileId, " +
                "id=$id, " +
                "type=$type, " +
                "date=$date, " +
                "lessonNumber=$lessonNumber, " +
                "startTime=$startTime, " +
                "endTime=$endTime, " +
                "subjectId=$subjectId, " +
                "teacherId=$teacherId, " +
                "teamId=$teamId, " +
                "classroom=$classroom, " +
                "oldDate=$oldDate, " +
                "oldLessonNumber=$oldLessonNumber, " +
                "oldStartTime=$oldStartTime, " +
                "oldEndTime=$oldEndTime, " +
                "oldSubjectId=$oldSubjectId, " +
                "oldTeacherId=$oldTeacherId, " +
                "oldTeamId=$oldTeamId, " +
                "oldClassroom=$oldClassroom)"
    }
}
/*
DROP TABLE lessons;
DROP TABLE lessonChanges;
CREATE TABLE lessons (
	profileId INTEGER NOT NULL,
	type INTEGER NOT NULL,

	date TEXT DEFAULT NULL,
	lessonNumber INTEGER DEFAULT NULL,
	startTime TEXT DEFAULT NULL,
	endTime TEXT DEFAULT NULL,
	teacherId INTEGER DEFAULT NULL,
	subjectId INTEGER DEFAULT NULL,
	teamId INTEGER DEFAULT NULL,
	classroom TEXT DEFAULT NULL,

	oldDate TEXT DEFAULT NULL,
	oldLessonNumber INTEGER DEFAULT NULL,
	oldStartTime TEXT DEFAULT NULL,
	oldEndTime TEXT DEFAULT NULL,
	oldTeacherId INTEGER DEFAULT NULL,
	oldSubjectId INTEGER DEFAULT NULL,
	oldTeamId INTEGER DEFAULT NULL,
	oldClassroom TEXT DEFAULT NULL,

	PRIMARY KEY(profileId)
);
*/