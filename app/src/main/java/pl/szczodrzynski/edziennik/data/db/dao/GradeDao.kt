/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */
package pl.szczodrzynski.edziennik.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import pl.szczodrzynski.edziennik.data.db.entity.Grade
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.full.GradeFull
import java.util.*
import kotlin.collections.List
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.collections.set

@Dao
abstract class GradeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun add(grade: Grade): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun addAll(gradeList: List<Grade>)

    @Query("DELETE FROM grades WHERE profileId = :profileId")
    abstract fun clear(profileId: Int)

    @Query("DELETE FROM grades WHERE profileId = :profileId AND gradeType = :type")
    abstract fun clearWithType(profileId: Int, type: Int)

    @Query("DELETE FROM grades WHERE profileId = :profileId AND gradeSemester = :semester")
    abstract fun clearForSemester(profileId: Int, semester: Int)

    @Query("DELETE FROM grades WHERE profileId = :profileId AND gradeSemester = :semester AND gradeType = :type")
    abstract fun clearForSemesterWithType(profileId: Int, semester: Int, type: Int)

    @RawQuery(observedEntities = [Grade::class])
    abstract fun getAll(query: SupportSQLiteQuery?): LiveData<List<GradeFull>>

    fun getAll(profileId: Int, filter: String, orderBy: String): LiveData<List<GradeFull>> {
        return getAll(SimpleSQLiteQuery("SELECT \n" +
                "*, \n" +
                "teachers.teacherName || ' ' || teachers.teacherSurname AS teacherFullName\n" +
                "FROM grades \n" +
                "LEFT JOIN subjects USING(profileId, subjectId)\n" +
                "LEFT JOIN teachers USING(profileId, teacherId)\n" +
                "LEFT JOIN metadata ON gradeId = thingId AND thingType = " + Metadata.TYPE_GRADE + " AND metadata.profileId = " + profileId + "\n" +
                "WHERE grades.profileId = " + profileId + " AND " + filter + "\n" +
                "ORDER BY " + orderBy)) // TODO: 2019-04-30 why did I add sorting by gradeType???
    }

    fun getAllOrderBy(profileId: Int, orderBy: String): LiveData<List<GradeFull>> {
        return getAll(profileId, "1", orderBy)
    }

    fun getAllWhere(profileId: Int, filter: String): LiveData<List<GradeFull>> {
        return getAll(profileId, filter, "addedDate DESC")
    }

    @RawQuery
    abstract fun getAllNow(query: SupportSQLiteQuery?): List<GradeFull>

    fun getAllNow(profileId: Int, filter: String): List<GradeFull> {
        return getAllNow(SimpleSQLiteQuery("SELECT \n" +
                "*, \n" +
                "teachers.teacherName || ' ' || teachers.teacherSurname AS teacherFullName\n" +
                "FROM grades \n" +
                "LEFT JOIN subjects USING(profileId, subjectId)\n" +
                "LEFT JOIN teachers USING(profileId, teacherId)\n" +
                "LEFT JOIN metadata ON gradeId = thingId AND thingType = " + Metadata.TYPE_GRADE + " AND metadata.profileId = " + profileId + "\n" +
                "WHERE grades.profileId = " + profileId + " AND " + filter + "\n" +
                "ORDER BY addedDate DESC"))
    }

    fun getNotNotifiedNow(profileId: Int): List<GradeFull> {
        return getAllNow(profileId, "notified = 0")
    }

    fun getAllWithParentIdNow(profileId: Int, parentId: Long): List<GradeFull> {
        return getAllNow(profileId, "gradeParentId = $parentId")
    }

    @get:Query("SELECT * FROM grades " +
            "LEFT JOIN subjects USING(profileId, subjectId) " +
            "LEFT JOIN metadata ON gradeId = thingId AND thingType = " + Metadata.TYPE_GRADE + " AND metadata.profileId = grades.profileId " +
            "WHERE notified = 0 " +
            "ORDER BY addedDate DESC")
    abstract val notNotifiedNow: List<GradeFull>

    @RawQuery
    abstract fun getNow(query: SupportSQLiteQuery): GradeFull?

    @Query("UPDATE grades SET gradeClassAverage = :classAverage, gradeColor = :color WHERE profileId = :profileId AND gradeId = :gradeId")
    abstract fun updateDetailsById(profileId: Int, gradeId: Long, classAverage: Float, color: Int)

    @Query("UPDATE metadata SET addedDate = :addedDate WHERE profileId = :profileId AND thingType = " + Metadata.TYPE_GRADE + " AND thingId = :gradeId")
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

    @Query("SELECT addedDate FROM metadata WHERE profileId = :profileId AND thingType = " + Metadata.TYPE_GRADE + " ORDER BY thingId")
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

    fun getAllFromDate(profileId: Int, date: Long): LiveData<List<GradeFull>> {
        return getAllWhere(profileId, "addedDate > $date")
    }
}
