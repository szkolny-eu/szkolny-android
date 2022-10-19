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
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.TeacherAbsence
import pl.szczodrzynski.edziennik.data.db.full.TeacherAbsenceFull
import pl.szczodrzynski.edziennik.utils.models.Date

@Dao
@SelectiveDao(db = AppDb::class)
abstract class TeacherAbsenceDao : BaseDao<TeacherAbsence, TeacherAbsenceFull> {
    companion object {
        private const val QUERY = """
            SELECT 
            *, 
            teachers.teacherName ||" "|| teachers.teacherSurname AS teacherName
            FROM teacherAbsence
            LEFT JOIN teachers USING(profileId, teacherId)
            LEFT JOIN metadata ON teacherAbsenceId = thingId AND thingType = 9 AND metadata.profileId = teacherAbsence.profileId
        """

        private const val ORDER_BY = """ORDER BY teacherAbsenceDateFrom ASC"""
    }

    private val selective by lazy { TeacherAbsenceDaoSelective(App.db) }

    @RawQuery(observedEntities = [TeacherAbsence::class])
    abstract override fun getRaw(query: SupportSQLiteQuery): LiveData<List<TeacherAbsenceFull>>
    @RawQuery(observedEntities = [TeacherAbsence::class])
    abstract override fun getOne(query: SupportSQLiteQuery): LiveData<TeacherAbsenceFull?>

    // SELECTIVE UPDATE
    @UpdateSelective(primaryKeys = ["profileId", "teacherAbsenceId"], skippedColumns = ["addedDate"])
    override fun update(item: TeacherAbsence) = selective.update(item)
    override fun updateAll(items: List<TeacherAbsence>) = selective.updateAll(items)

    // CLEAR
    @Query("DELETE FROM teacherAbsence WHERE profileId = :profileId")
    abstract override fun clear(profileId: Int)
    // REMOVE NOT KEPT
    @Query("DELETE FROM teacherAbsence WHERE keep = 0")
    abstract override fun removeNotKept()

    // GET ALL - LIVE DATA
    fun getAll(profileId: Int) =
            getRaw("$QUERY WHERE teacherAbsence.profileId = $profileId $ORDER_BY")
    fun getAllByDate(profileId: Int, date: Date) =
            getRaw("$QUERY WHERE teacherAbsence.profileId = $profileId AND '${date.stringY_m_d}' BETWEEN teacherAbsenceDateFrom AND teacherAbsenceDateTo $ORDER_BY")

    // GET ALL - NOW
    fun getAllNow(profileId: Int) =
            getRawNow("$QUERY WHERE teacherAbsence.profileId = $profileId $ORDER_BY")
    fun getNotNotifiedNow() =
            getRawNow("$QUERY WHERE notified = 0 $ORDER_BY")
    fun getNotNotifiedNow(profileId: Int) =
            getRawNow("$QUERY WHERE teacherAbsence.profileId = $profileId AND notified = 0 $ORDER_BY")
    fun getAllByDateNow(profileId: Int, date: Date) =
            getRawNow("$QUERY WHERE teacherAbsence.profileId = $profileId AND '${date.stringY_m_d}' BETWEEN teacherAbsenceDateFrom AND teacherAbsenceDateTo $ORDER_BY")

    // GET ONE - NOW
    fun getByIdNow(profileId: Int, id: Long) =
            getOneNow("$QUERY WHERE teacherAbsence.profileId = $profileId AND teacherAbsenceId = $id")
}
