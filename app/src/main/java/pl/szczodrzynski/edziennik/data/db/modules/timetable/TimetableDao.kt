package pl.szczodrzynski.edziennik.data.db.modules.timetable

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.utils.models.Date

@Dao
interface TimetableDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    operator fun plusAssign(lessonList: List<Lesson>)

    @Query("DELETE FROM timetable WHERE profileId = :profileId")
    fun clear(profileId: Int)

    @Query("""
        SELECT 
        timetable.*,
        subjects.subjectLongName AS subjectName,
        teachers.teacherName ||" "|| teachers.teacherSurname AS teacherName,
        teams.teamName AS teamName,
        oldS.subjectLongName AS oldSubjectName,
        oldT.teacherName ||" "|| oldT.teacherSurname AS oldTeacherName,
        oldG.teamName AS oldTeamName,
        metadata.seen, metadata.notified, metadata.addedDate
        FROM timetable
        LEFT JOIN subjects USING(profileId, subjectId)
        LEFT JOIN teachers USING(profileId, teacherId)
        LEFT JOIN teams USING(profileId, teamId)
        LEFT JOIN subjects AS oldS ON timetable.profileId = oldS.profileId AND timetable.oldSubjectId = oldS.subjectId
        LEFT JOIN teachers AS oldT ON timetable.profileId = oldT.profileId AND timetable.oldTeacherId = oldT.teacherId
        LEFT JOIN teams AS oldG ON timetable.profileId = oldG.profileId AND timetable.oldTeamId = oldG.teamId
        LEFT JOIN metadata ON id = thingId AND thingType = ${Metadata.TYPE_LESSON_CHANGE} AND metadata.profileId = timetable.profileId
        WHERE timetable.profileId = :profileId AND (type != 3 AND date = :date) OR ((type = 3 OR type = 1) AND oldDate = :date)
        ORDER BY type
    """)
    fun getForDate(profileId: Int, date: Date) : LiveData<List<LessonFull>>
}
