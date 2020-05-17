/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-25.
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
import pl.szczodrzynski.edziennik.data.db.entity.LuckyNumber
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.full.LuckyNumberFull
import pl.szczodrzynski.edziennik.utils.models.Date

@Dao
@SelectiveDao(db = AppDb::class)
abstract class LuckyNumberDao : BaseDao<LuckyNumber, LuckyNumberFull> {
    companion object {
        private const val QUERY = """
            SELECT 
            * 
            FROM luckyNumbers
            LEFT JOIN metadata ON luckyNumberDate = thingId AND thingType = ${Metadata.TYPE_LUCKY_NUMBER} AND metadata.profileId = luckyNumbers.profileId
        """

        private const val ORDER_BY = """ORDER BY luckyNumberDate DESC"""
    }

    private val selective by lazy { LuckyNumberDaoSelective(App.db) }

    @RawQuery(observedEntities = [LuckyNumber::class])
    abstract override fun getRaw(query: SupportSQLiteQuery): LiveData<List<LuckyNumberFull>>
    @RawQuery(observedEntities = [LuckyNumber::class])
    abstract override fun getOne(query: SupportSQLiteQuery): LiveData<LuckyNumberFull?>

    // SELECTIVE UPDATE
    @UpdateSelective(primaryKeys = ["profileId", "luckyNumberDate"], skippedColumns = ["addedDate"])
    override fun update(item: LuckyNumber) = selective.update(item)
    override fun updateAll(items: List<LuckyNumber>) = selective.updateAll(items)

    // CLEAR
    @Query("DELETE FROM luckyNumbers WHERE profileId = :profileId")
    abstract override fun clear(profileId: Int)
    // REMOVE NOT KEPT
    @Query("DELETE FROM luckyNumbers WHERE keep = 0")
    abstract override fun removeNotKept()

    // GET ALL - LIVE DATA
    fun getAll(profileId: Int) =
            getRaw("$QUERY WHERE luckyNumbers.profileId = $profileId $ORDER_BY")

    // GET ALL - NOW
    fun getAllNow(profileId: Int) =
            getRawNow("$QUERY WHERE luckyNumbers.profileId = $profileId $ORDER_BY")
    fun getNotNotifiedNow() =
            getRawNow("$QUERY WHERE notified = 0 $ORDER_BY")
    fun getNotNotifiedNow(profileId: Int) =
            getRawNow("$QUERY WHERE luckyNumbers.profileId = $profileId AND notified = 0 $ORDER_BY")

    // GET ONE - LIVE DATA
    fun getNearestFuture(profileId: Int, today: Date) =
            getOne("$QUERY WHERE luckyNumbers.profileId = $profileId AND luckyNumberDate >= ${today.value} $ORDER_BY LIMIT 1")

    // GET ONE - NOW
    fun getByIdNow(profileId: Int, id: Long) =
            getOneNow("$QUERY WHERE attendances.profileId = $profileId AND attendanceId = $id")
    fun getNearestFutureNow(profileId: Int, today: Date) =
            getOneNow("$QUERY WHERE luckyNumbers.profileId = $profileId AND luckyNumberDate >= ${today.value} $ORDER_BY LIMIT 1")
}
