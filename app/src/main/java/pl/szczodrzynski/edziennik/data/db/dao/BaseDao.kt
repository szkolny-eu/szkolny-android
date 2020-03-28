/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-28.
 */

package pl.szczodrzynski.edziennik.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
interface BaseDao<T, F> {
    @RawQuery
    fun getRaw(query: SupportSQLiteQuery): LiveData<List<F>>
    fun getRaw(query: String) = getRaw(SimpleSQLiteQuery(query))
    @RawQuery
    fun getRawNow(query: SupportSQLiteQuery): List<F>
    fun getRawNow(query: String) = getRawNow(SimpleSQLiteQuery(query))
    @RawQuery
    fun getOneNow(query: SupportSQLiteQuery): F?
    fun getOneNow(query: String) = getOneNow(SimpleSQLiteQuery(query))

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(item: T): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAll(items: List<T>): LongArray

    fun clear(profileId: Int)
}
