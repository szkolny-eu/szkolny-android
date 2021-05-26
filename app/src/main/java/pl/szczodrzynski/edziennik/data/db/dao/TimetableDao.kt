/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-25.
 */
package pl.szczodrzynski.edziennik.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import eu.szkolny.selectivedao.annotation.SelectiveDao
import eu.szkolny.selectivedao.annotation.UpdateSelective
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.db.AppDb
import pl.szczodrzynski.edziennik.data.db.entity.Lesson
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.full.LessonFull
import pl.szczodrzynski.edziennik.utils.models.Date

@Dao
@SelectiveDao(db = AppDb::class)
abstract class TimetableDao : BaseDao<Lesson, LessonFull> {
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
            metadata.seen, metadata.notified
            FROM timetable
            LEFT JOIN subjects USING(profileId, subjectId)
            LEFT JOIN teachers USING(profileId, teacherId)
            LEFT JOIN teams USING(profileId, teamId)
            LEFT JOIN subjects AS oldS ON timetable.profileId = oldS.profileId AND timetable.oldSubjectId = oldS.subjectId
            LEFT JOIN teachers AS oldT ON timetable.profileId = oldT.profileId AND timetable.oldTeacherId = oldT.teacherId
            LEFT JOIN teams AS oldG ON timetable.profileId = oldG.profileId AND timetable.oldTeamId = oldG.teamId
            LEFT JOIN metadata ON id = thingId AND thingType = ${Metadata.TYPE_LESSON_CHANGE} AND metadata.profileId = timetable.profileId
        """

        private const val ORDER_BY = """ORDER BY profileId, id, type"""
        private const val IS_CHANGED = """type != -1 AND type != 0"""
    }

    private val selective by lazy { TimetableDaoSelective(App.db) }

    @RawQuery(observedEntities = [Lesson::class])
    abstract override fun getRaw(query: SupportSQLiteQuery): LiveData<List<LessonFull>>
    @RawQuery(observedEntities = [Lesson::class])
    abstract override fun getOne(query: SupportSQLiteQuery): LiveData<LessonFull?>

    // SELECTIVE UPDATE
    @UpdateSelective(primaryKeys = ["profileId", "id"], skippedColumns = ["addedDate"])
    override fun update(item: Lesson) = selective.update(item)
    override fun updateAll(items: List<Lesson>) = selective.updateAll(items)

    // CLEAR
    @Query("DELETE FROM timetable WHERE profileId = :profileId")
    abstract override fun clear(profileId: Int)
    // REMOVE NOT KEPT
    @Query("DELETE FROM timetable WHERE keep = 0")
    abstract override fun removeNotKept()

    // GET ALL - LIVE DATA
    fun getAll(profileId: Int) =
            getRaw("$QUERY WHERE timetable.profileId = $profileId $ORDER_BY")
    fun getAllForDate(profileId: Int, date: Date) =
            getRaw("$QUERY WHERE timetable.profileId = $profileId AND ((type != 3 AND date = '${date.stringY_m_d}') OR ((type = 3 OR type = 1) AND oldDate = '${date.stringY_m_d}')) $ORDER_BY")
    fun getNextWithSubject(profileId: Int, date: Date, subjectId: Long) =
            getOne("$QUERY " +
                    "WHERE timetable.profileId = $profileId " +
                    "AND ((type != 3 AND date > '${date.stringY_m_d}') OR ((type = 3 OR type = 1) AND oldDate > '${date.stringY_m_d}')) " +
                    "AND timetable.subjectId = $subjectId " +
                    "LIMIT 1")
    fun getNextWithSubjectAndTeam(profileId: Int, date: Date, subjectId: Long, teamId: Long) =
            getOne("$QUERY " +
                    "WHERE timetable.profileId = $profileId " +
                    "AND ((type != 3 AND date > '${date.stringY_m_d}') OR ((type = 3 OR type = 1) AND oldDate > '${date.stringY_m_d}')) " +
                    "AND timetable.subjectId = $subjectId " +
                    "AND timetable.teamId = $teamId " +
                    "LIMIT 1")
    fun getBetweenDates(dateFrom: Date, dateTo: Date) =
            getRaw("$QUERY WHERE (type != 3 AND date >= '${dateFrom.stringY_m_d}' AND date <= '${dateTo.stringY_m_d}') OR ((type = 3 OR type = 1) AND oldDate >= '${dateFrom.stringY_m_d}' AND oldDate <= '${dateTo.stringY_m_d}') $ORDER_BY")
    fun getChanges(profileId: Int) =
            getRaw("$QUERY WHERE timetable.profileId = $profileId AND $IS_CHANGED $ORDER_BY")

    // GET ALL - NOW
    fun getAllNow(profileId: Int) =
            getRawNow("$QUERY WHERE timetable.profileId = $profileId $ORDER_BY")
    fun getNotNotifiedNow() =
            getRawNow("$QUERY WHERE notified = 0 AND timetable.type NOT IN (${Lesson.TYPE_NORMAL}, ${Lesson.TYPE_NO_LESSONS}, ${Lesson.TYPE_SHIFTED_SOURCE}) $ORDER_BY")
    fun getNotNotifiedNow(profileId: Int) =
            getRawNow("$QUERY WHERE timetable.profileId = $profileId AND notified = 0 AND timetable.type NOT IN (${Lesson.TYPE_NORMAL}, ${Lesson.TYPE_NO_LESSONS}, ${Lesson.TYPE_SHIFTED_SOURCE}) $ORDER_BY")
    fun getAllForDateNow(profileId: Int, date: Date) =
            getRawNow("$QUERY WHERE timetable.profileId = $profileId AND ((type != 3 AND date = '${date.stringY_m_d}') OR ((type = 3 OR type = 1) AND oldDate = '${date.stringY_m_d}')) $ORDER_BY")
    fun getChangesNow(profileId: Int) =
            getRawNow("$QUERY WHERE timetable.profileId = $profileId AND $IS_CHANGED $ORDER_BY")
    fun getChangesForDateNow(profileId: Int, date: Date) =
            getRawNow("$QUERY WHERE timetable.profileId = $profileId AND $IS_CHANGED AND ((type != 3 AND date = '${date.stringY_m_d}') OR ((type = 3 OR type = 1) AND oldDate = '${date.stringY_m_d}')) $ORDER_BY")
    fun getBetweenDatesNow(dateFrom: Date, dateTo: Date) =
            getRawNow("$QUERY WHERE (type != 3 AND date >= '${dateFrom.stringY_m_d}' AND date <= '${dateTo.stringY_m_d}') OR ((type = 3 OR type = 1) AND oldDate >= '${dateFrom.stringY_m_d}' AND oldDate <= '${dateTo.stringY_m_d}') $ORDER_BY")

    // GET ONE - NOW
    fun getByIdNow(profileId: Int, id: Long) =
            getOneNow("$QUERY WHERE timetable.profileId = $profileId AND timetable.id = $id")

    @Query("UPDATE timetable SET keep = 0 WHERE profileId = :profileId AND type != -1 AND ((type != 3 AND date >= :dateFrom) OR ((type = 3 OR type = 1) AND oldDate >= :dateFrom))")
    abstract fun dontKeepFromDate(profileId: Int, dateFrom: Date)

    @Query("UPDATE timetable SET keep = 0 WHERE profileId = :profileId AND type != -1 AND ((type != 3 AND date <= :dateTo) OR ((type = 3 OR type = 1) AND oldDate <= :dateTo))")
    abstract fun dontKeepToDate(profileId: Int, dateTo: Date)

    @Query("UPDATE timetable SET keep = 0 WHERE profileId = :profileId AND type != -1 AND ((type != 3 AND date >= :dateFrom AND date <= :dateTo) OR ((type = 3 OR type = 1) AND oldDate >= :dateFrom AND oldDate <= :dateTo))")
    abstract fun dontKeepBetweenDates(profileId: Int, dateFrom: Date, dateTo: Date)
}
