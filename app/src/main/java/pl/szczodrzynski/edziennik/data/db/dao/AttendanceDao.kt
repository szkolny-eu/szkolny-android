/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-24.
 */
package pl.szczodrzynski.edziennik.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.annotation.SelectiveDao
import pl.szczodrzynski.edziennik.annotation.UpdateSelective
import pl.szczodrzynski.edziennik.data.db.AppDb
import pl.szczodrzynski.edziennik.data.db.entity.Attendance
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.full.AttendanceFull
import pl.szczodrzynski.edziennik.utils.models.Date

@Dao
@SelectiveDao(db = AppDb::class)
abstract class AttendanceDao : BaseDao<Attendance, AttendanceFull> {
    companion object {
        private const val QUERY = """
            SELECT 
            *, 
            teachers.teacherName ||" "|| teachers.teacherSurname AS teacherName
            FROM attendances
            LEFT JOIN teachers USING(profileId, teacherId)
            LEFT JOIN subjects USING(profileId, subjectId)
            LEFT JOIN metadata ON attendanceId = thingId AND thingType = ${Metadata.TYPE_ATTENDANCE} AND metadata.profileId = attendances.profileId
        """

        private const val ORDER_BY = """ORDER BY attendanceDate DESC, attendanceTime DESC"""
    }

    private val selective by lazy { AttendanceDaoSelective(App.db) }

    @RawQuery(observedEntities = [Attendance::class])
    abstract override fun getRaw(query: SupportSQLiteQuery): LiveData<List<AttendanceFull>>
    @RawQuery(observedEntities = [Attendance::class])
    abstract override fun getOne(query: SupportSQLiteQuery): LiveData<AttendanceFull?>

    // SELECTIVE UPDATE
    @UpdateSelective(primaryKeys = ["profileId", "attendanceId"], skippedColumns = ["addedDate", "announcementText"])
    override fun update(item: Attendance) = selective.update(item)
    override fun updateAll(items: List<Attendance>) = selective.updateAll(items)

    // CLEAR
    @Query("DELETE FROM attendances WHERE profileId = :profileId")
    abstract override fun clear(profileId: Int)
    // REMOVE NOT KEPT
    @Query("DELETE FROM attendances WHERE keep = 0")
    abstract override fun removeNotKept()

    // GET ALL - LIVE DATA
    fun getAll(profileId: Int) =
            getRaw("$QUERY WHERE attendances.profileId = $profileId $ORDER_BY")

    // GET ALL - NOW
    fun getAllNow(profileId: Int) =
            getRawNow("$QUERY WHERE attendances.profileId = $profileId $ORDER_BY")
    fun getNotNotifiedNow() =
            getRawNow("$QUERY WHERE notified = 0 $ORDER_BY")
    fun getNotNotifiedNow(profileId: Int) =
            getRawNow("$QUERY WHERE attendances.profileId = $profileId AND notified = 0 $ORDER_BY")

    // GET ONE - NOW
    fun getByIdNow(profileId: Int, id: Long) =
            getOneNow("$QUERY WHERE attendances.profileId = $profileId AND attendanceId = $id")

    @Query("UPDATE attendances SET keep = 0 WHERE profileId = :profileId AND attendanceDate >= :date")
    abstract fun dontKeepAfterDate(profileId: Int, date: Date?)
}
