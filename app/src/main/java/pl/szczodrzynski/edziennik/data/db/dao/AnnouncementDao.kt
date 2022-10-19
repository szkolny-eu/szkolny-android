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
import pl.szczodrzynski.edziennik.data.db.entity.Announcement
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.enums.MetadataType
import pl.szczodrzynski.edziennik.data.db.full.AnnouncementFull

@Dao
@SelectiveDao(db = AppDb::class)
abstract class AnnouncementDao : BaseDao<Announcement, AnnouncementFull> {
    companion object {
        private const val QUERY = """
            SELECT 
            *, 
            teachers.teacherName ||" "|| teachers.teacherSurname AS teacherName
            FROM announcements
            LEFT JOIN teachers USING(profileId, teacherId)
            LEFT JOIN metadata ON announcementId = thingId AND thingType = 7 AND metadata.profileId = announcements.profileId
        """

        private const val ORDER_BY = """ORDER BY addedDate DESC"""
    }

    private val selective by lazy { AnnouncementDaoSelective(App.db) }

    @RawQuery(observedEntities = [Announcement::class])
    abstract override fun getRaw(query: SupportSQLiteQuery): LiveData<List<AnnouncementFull>>
    @RawQuery(observedEntities = [Announcement::class])
    abstract override fun getOne(query: SupportSQLiteQuery): LiveData<AnnouncementFull?>

    // SELECTIVE UPDATE
    @UpdateSelective(primaryKeys = ["profileId", "announcementId"], skippedColumns = ["addedDate", "announcementText"])
    override fun update(item: Announcement) = selective.update(item)
    override fun updateAll(items: List<Announcement>) = selective.updateAll(items)

    // CLEAR
    @Query("DELETE FROM announcements WHERE profileId = :profileId")
    abstract override fun clear(profileId: Int)
    // REMOVE NOT KEPT
    @Query("DELETE FROM announcements WHERE keep = 0")
    abstract override fun removeNotKept()

    // GET ALL - LIVE DATA
    fun getAll(profileId: Int) =
            getRaw("$QUERY WHERE announcements.profileId = $profileId $ORDER_BY")

    // GET ALL - NOW
    fun getAllNow(profileId: Int) =
            getRawNow("$QUERY WHERE announcements.profileId = $profileId $ORDER_BY")
    fun getNotNotifiedNow() =
            getRawNow("$QUERY WHERE notified = 0 $ORDER_BY")
    fun getNotNotifiedNow(profileId: Int) =
            getRawNow("$QUERY WHERE announcements.profileId = $profileId AND notified = 0 $ORDER_BY")

    // GET ONE - NOW
    fun getByIdNow(profileId: Int, id: Long) =
            getOneNow("$QUERY WHERE announcements.profileId = $profileId AND announcementId = $id")
}
