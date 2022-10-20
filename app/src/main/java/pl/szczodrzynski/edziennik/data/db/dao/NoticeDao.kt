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
import pl.szczodrzynski.edziennik.data.db.entity.Notice
import pl.szczodrzynski.edziennik.data.db.full.NoticeFull

@Dao
@SelectiveDao(db = AppDb::class)
abstract class NoticeDao : BaseDao<Notice, NoticeFull> {
    companion object {
        private const val QUERY = """
            SELECT 
            *, 
            teachers.teacherName ||" "|| teachers.teacherSurname AS teacherName
            FROM notices
            LEFT JOIN teachers USING(profileId, teacherId)
            LEFT JOIN metadata ON noticeId = thingId AND thingType = 2 AND metadata.profileId = notices.profileId
        """

        private const val ORDER_BY = """ORDER BY addedDate DESC"""
    }

    private val selective by lazy { NoticeDaoSelective(App.db) }

    @RawQuery(observedEntities = [Notice::class])
    abstract override fun getRaw(query: SupportSQLiteQuery): LiveData<List<NoticeFull>>
    @RawQuery(observedEntities = [Notice::class])
    abstract override fun getOne(query: SupportSQLiteQuery): LiveData<NoticeFull?>

    // SELECTIVE UPDATE
    @UpdateSelective(primaryKeys = ["profileId", "noticeId"], skippedColumns = ["addedDate"])
    override fun update(item: Notice) = selective.update(item)
    override fun updateAll(items: List<Notice>) = selective.updateAll(items)

    // CLEAR
    @Query("DELETE FROM notices WHERE profileId = :profileId")
    abstract override fun clear(profileId: Int)
    // REMOVE NOT KEPT
    @Query("DELETE FROM notices WHERE keep = 0")
    abstract override fun removeNotKept()

    // GET ALL - LIVE DATA
    fun getAll(profileId: Int) =
            getRaw("$QUERY WHERE notices.profileId = $profileId $ORDER_BY")

    // GET ALL - NOW
    fun getAllNow(profileId: Int) =
            getRawNow("$QUERY WHERE notices.profileId = $profileId $ORDER_BY")
    fun getNotNotifiedNow() =
            getRawNow("$QUERY WHERE notified = 0 $ORDER_BY")
    fun getNotNotifiedNow(profileId: Int) =
            getRawNow("$QUERY WHERE notices.profileId = $profileId AND notified = 0 $ORDER_BY")

    // GET ONE - NOW
    fun getByIdNow(profileId: Int, id: Long) =
            getOneNow("$QUERY WHERE notices.profileId = $profileId AND noticeId = $id")

    @Query("UPDATE notices SET keep = 0 WHERE profileId = :profileId AND noticeSemester = :semester")
    abstract fun dontKeepSemester(profileId: Int, semester: Int)
}
