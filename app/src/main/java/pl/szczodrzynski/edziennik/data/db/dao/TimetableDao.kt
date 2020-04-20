/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */

package pl.szczodrzynski.edziennik.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import pl.szczodrzynski.edziennik.data.db.entity.Lesson
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.full.LessonFull
import pl.szczodrzynski.edziennik.utils.models.Date

@Dao
interface TimetableDao {
    companion object {
        private const val QUERY = """
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
        """
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    operator fun plusAssign(lessonList: List<Lesson>)

    @Query("DELETE FROM timetable WHERE profileId = :profileId")
    fun clear(profileId: Int)

    @Query("DELETE FROM timetable WHERE profileId = :profileId AND type != -1 AND ((type != 3 AND date >= :dateFrom) OR ((type = 3 OR type = 1) AND oldDate >= :dateFrom))")
    fun clearFromDate(profileId: Int, dateFrom: Date)

    @Query("DELETE FROM timetable WHERE profileId = :profileId AND type != -1 AND ((type != 3 AND date <= :dateTo) OR ((type = 3 OR type = 1) AND oldDate <= :dateTo))")
    fun clearToDate(profileId: Int, dateTo: Date)

    @Query("DELETE FROM timetable WHERE profileId = :profileId AND type != -1 AND ((type != 3 AND date >= :dateFrom AND date <= :dateTo) OR ((type = 3 OR type = 1) AND oldDate >= :dateFrom AND oldDate <= :dateTo))")
    fun clearBetweenDates(profileId: Int, dateFrom: Date, dateTo: Date)

    @RawQuery(observedEntities = [Lesson::class])
    fun getRaw(query: SupportSQLiteQuery): LiveData<List<LessonFull>>

    @Query("""
        $QUERY
        WHERE timetable.profileId = :profileId AND type != -1 AND type != 0
        ORDER BY id, type
    """)
    fun getAllChangesNow(profileId: Int): List<LessonFull>

    @Query("""
        $QUERY
        WHERE timetable.profileId = :profileId AND type != -1 AND type != 0 AND ((type != 3 AND date = :date) OR ((type = 3 OR type = 1) AND oldDate = :date))
        ORDER BY id, type
    """)
    fun getChangesForDateNow(profileId: Int, date: Date): List<LessonFull>

    fun getForDate(profileId: Int, date: Date) = getRaw(SimpleSQLiteQuery("""
        $QUERY
        WHERE timetable.profileId = $profileId AND ((type != 3 AND date = "${date.stringY_m_d}") OR ((type = 3 OR type = 1) AND oldDate = "${date.stringY_m_d}"))
        ORDER BY id, type
    """))

    @Query("""
        $QUERY
        WHERE timetable.profileId = :profileId AND ((type != 3 AND date = :date) OR ((type = 3 OR type = 1) AND oldDate = :date))
        ORDER BY id, type
    """)
    fun getForDateNow(profileId: Int, date: Date): List<LessonFull>

    @Query("""
        $QUERY
        WHERE timetable.profileId = :profileId AND ((type != 3 AND date > :today) OR ((type = 3 OR type = 1) AND oldDate > :today)) AND timetable.subjectId = :subjectId
        ORDER BY id, type
        LIMIT 1
    """)
    fun getNextWithSubject(profileId: Int, today: Date, subjectId: Long): LiveData<LessonFull?>

    @Query("""
        $QUERY
        WHERE timetable.profileId = :profileId AND ((type != 3 AND date > :today) OR ((type = 3 OR type = 1) AND oldDate > :today)) AND timetable.subjectId = :subjectId AND timetable.teamId = :teamId
        ORDER BY id, type
        LIMIT 1
    """)
    fun getNextWithSubjectAndTeam(profileId: Int, today: Date, subjectId: Long, teamId: Long): LiveData<LessonFull?>

    @Query("""
        $QUERY
        WHERE (type != 3 AND date >= :dateFrom AND date <= :dateTo) OR ((type = 3 OR type = 1) AND oldDate >= :dateFrom AND oldDate <= :dateTo)
        ORDER BY profileId, id, type
    """)
    fun getBetweenDatesNow(dateFrom: Date, dateTo: Date): List<LessonFull>

    @Query("""
        $QUERY
        WHERE (type != 3 AND date >= :dateFrom AND date <= :dateTo) OR ((type = 3 OR type = 1) AND oldDate >= :dateFrom AND oldDate <= :dateTo)
        ORDER BY profileId, id, type
    """)
    fun getBetweenDates(dateFrom: Date, dateTo: Date): LiveData<List<LessonFull>>

    @Query("""
        $QUERY
        WHERE timetable.profileId = :profileId AND timetable.id = :lessonId
        ORDER BY id, type
    """)
    fun getByIdNow(profileId: Int, lessonId: Long): LessonFull?

    @Query("""
        $QUERY
        WHERE timetable.profileId = :profileId AND timetable.type NOT IN (${Lesson.TYPE_NORMAL}, ${Lesson.TYPE_NO_LESSONS}, ${Lesson.TYPE_SHIFTED_SOURCE}) AND metadata.notified = 0
    """)
    fun getNotNotifiedNow(profileId: Int): List<LessonFull>

    @Query("""
        SELECT 
        timetable.*,
        subjects.subjectLongName AS subjectName,
        teachers.teacherName ||" "|| teachers.teacherSurname AS teacherName,
        oldS.subjectLongName AS oldSubjectName,
        oldT.teacherName ||" "|| oldT.teacherSurname AS oldTeacherName,
        metadata.seen, metadata.notified, metadata.addedDate
        FROM timetable
        LEFT JOIN subjects USING(profileId, subjectId)
        LEFT JOIN teachers USING(profileId, teacherId)
        LEFT JOIN subjects AS oldS ON timetable.profileId = oldS.profileId AND timetable.oldSubjectId = oldS.subjectId
        LEFT JOIN teachers AS oldT ON timetable.profileId = oldT.profileId AND timetable.oldTeacherId = oldT.teacherId
        LEFT JOIN metadata ON id = thingId AND thingType = ${Metadata.TYPE_LESSON_CHANGE} AND metadata.profileId = timetable.profileId
        WHERE timetable.type NOT IN (${Lesson.TYPE_NORMAL}, ${Lesson.TYPE_NO_LESSONS}, ${Lesson.TYPE_SHIFTED_SOURCE}) AND metadata.notified = 0
    """)
    fun getNotNotifiedNow(): List<LessonFull>
}
