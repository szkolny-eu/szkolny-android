/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-24.
 */
package pl.szczodrzynski.edziennik.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.annotation.SelectiveDao
import pl.szczodrzynski.edziennik.annotation.UpdateSelective
import pl.szczodrzynski.edziennik.data.db.AppDb
import pl.szczodrzynski.edziennik.data.db.entity.Grade
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.full.GradeFull
import pl.szczodrzynski.edziennik.utils.models.Date
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

@Dao
@SelectiveDao(db = AppDb::class)
abstract class GradeDao : BaseDao<Grade, GradeFull> {
    companion object {
        private const val QUERY = """
            SELECT 
            *, 
            teachers.teacherName ||" "|| teachers.teacherSurname AS teacherName
            FROM grades
            LEFT JOIN teachers USING(profileId, teacherId)
            LEFT JOIN subjects USING(profileId, subjectId)
            LEFT JOIN metadata ON gradeId = thingId AND thingType = ${Metadata.TYPE_GRADE} AND metadata.profileId = grades.profileId
        """

        private const val ORDER_BY = """ORDER BY addedDate DESC"""
    }

    private val selective by lazy { GradeDaoSelective(App.db) }

    @RawQuery(observedEntities = [Grade::class])
    abstract override fun getRaw(query: SupportSQLiteQuery): LiveData<List<GradeFull>>
    @RawQuery(observedEntities = [Grade::class])
    abstract override fun getOne(query: SupportSQLiteQuery): LiveData<GradeFull?>

    // SELECTIVE UPDATE
    @UpdateSelective(primaryKeys = ["profileId", "gradeId"], skippedColumns = ["addedDate", "gradeClassAverage"])
    override fun update(item: Grade) = selective.update(item)
    override fun updateAll(items: List<Grade>) = selective.updateAll(items)

    // CLEAR
    @Query("DELETE FROM grades WHERE profileId = :profileId")
    abstract override fun clear(profileId: Int)
    // REMOVE NOT KEPT
    @Query("DELETE FROM grades WHERE keep = 0")
    abstract override fun removeNotKept()

    // GET ALL - LIVE DATA
    fun getAll(profileId: Int) =
            getRaw("$QUERY WHERE grades.profileId = $profileId $ORDER_BY")
    fun getAllFromDate(profileId: Int, date: Date) =
            getRaw("$QUERY WHERE grades.profileId = $profileId AND addedDate > ${date.inMillis} $ORDER_BY")
    fun getAllBySubject(profileId: Int, subjectId: Long) =
            getRaw("$QUERY WHERE grades.profileId = $profileId AND subjectId = $subjectId $ORDER_BY")
    fun getAllOrderBy(profileId: Int, orderBy: String) =
            getRaw("$QUERY WHERE grades.profileId = $profileId ORDER BY $orderBy")

    // GET ALL - NOW
    fun getAllNow(profileId: Int) =
            getRawNow("$QUERY WHERE grades.profileId = $profileId $ORDER_BY")
    fun getNotNotifiedNow() =
            getRawNow("$QUERY WHERE notified = 0 $ORDER_BY")
    fun getNotNotifiedNow(profileId: Int) =
            getRawNow("$QUERY WHERE grades.profileId = $profileId AND notified = 0 $ORDER_BY")
    fun getByParentIdNow(profileId: Int, parentId: Long) =
            getRawNow("$QUERY WHERE grades.profileId = $profileId AND gradeParentId = $parentId $ORDER_BY")

    // GET ONE - NOW
    fun getByIdNow(profileId: Int, id: Long) =
            getOneNow("$QUERY WHERE grades.profileId = $profileId AND gradeId = $id")

    @Query("UPDATE grades SET keep = 0 WHERE profileId = :profileId AND gradeType = :type")
    abstract fun dontKeepWithType(profileId: Int, type: Int)

    @Query("UPDATE grades SET keep = 0 WHERE profileId = :profileId AND gradeSemester = :semester")
    abstract fun dontKeepForSemester(profileId: Int, semester: Int)

    @Query("UPDATE grades SET keep = 0 WHERE profileId = :profileId AND gradeSemester = :semester AND gradeType = :type")
    abstract fun dontKeepForSemesterWithType(profileId: Int, semester: Int, type: Int)



    // GRADE DETAILS - MOBIDZIENNIK
    @Query("UPDATE grades SET gradeClassAverage = :classAverage, gradeColor = :color WHERE profileId = :profileId AND gradeId = :gradeId")
    abstract fun updateDetailsById(profileId: Int, gradeId: Long, classAverage: Float, color: Int)

    @Query("UPDATE grades SET addedDate = :addedDate WHERE profileId = :profileId AND gradeId = :gradeId")
    abstract fun updateAddedDateById(profileId: Int, gradeId: Long, addedDate: Long)

    @Transaction
    open fun updateDetails(profileId: Int, gradeAverages: SortedMap<Long, Float>, gradeAddedDates: SortedMap<Long, Long>, gradeColors: SortedMap<Long, Int>) {
        for ((gradeId, addedDate) in gradeAddedDates) {
            val classAverage = gradeAverages[gradeId] ?: 0.0f
            val color = gradeColors[gradeId] ?: 0xff2196f3.toInt()
            updateDetailsById(profileId, gradeId, classAverage, color)
            updateAddedDateById(profileId, gradeId, addedDate)
        }
    }

    @Query("SELECT gradeId FROM grades WHERE profileId = :profileId ORDER BY gradeId")
    abstract fun getIds(profileId: Int): List<Long>

    @Query("SELECT gradeClassAverage FROM grades WHERE profileId = :profileId ORDER BY gradeId")
    abstract fun getClassAverages(profileId: Int): List<Float>

    @Query("SELECT gradeColor FROM grades WHERE profileId = :profileId ORDER BY gradeId")
    abstract fun getColors(profileId: Int): List<Int>

    @Query("SELECT addedDate FROM grades WHERE profileId = :profileId ORDER BY gradeId")
    abstract fun getAddedDates(profileId: Int): List<Long>

    @Transaction
    open fun getDetails(profileId: Int, gradeAddedDates: SortedMap<Long, Long>, gradeAverages: SortedMap<Long, Float>, gradeColors: SortedMap<Long, Int>) {
        val ids = getIds(profileId).iterator()
        val classAverages = getClassAverages(profileId).iterator()
        val colors = getColors(profileId).iterator()
        val addedDates = getAddedDates(profileId).iterator()
        while (ids.hasNext() && classAverages.hasNext() && colors.hasNext() && addedDates.hasNext()) {
            val gradeId = ids.next()
            gradeAverages[gradeId] = classAverages.next()
            gradeColors[gradeId] = colors.next()
            gradeAddedDates[gradeId] = addedDates.next()
        }
    }
}
