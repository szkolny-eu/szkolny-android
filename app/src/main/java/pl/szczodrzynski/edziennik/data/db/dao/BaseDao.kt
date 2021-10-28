/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-28.
 */

package pl.szczodrzynski.edziennik.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import pl.szczodrzynski.edziennik.data.db.entity.Keepable

@Dao
interface BaseDao<T : Keepable, F : T> {
    @Transaction
    @RawQuery
    fun getRaw(query: SupportSQLiteQuery): LiveData<List<F>>
    fun getRaw(query: String) = getRaw(SimpleSQLiteQuery(query))
    @Transaction
    @RawQuery
    fun getOne(query: SupportSQLiteQuery): LiveData<F?>
    fun getOne(query: String) = getOne(SimpleSQLiteQuery(query))
    @Transaction
    @RawQuery
    fun getRawNow(query: SupportSQLiteQuery): List<F>
    fun getRawNow(query: String) = getRawNow(SimpleSQLiteQuery(query))
    @Transaction
    @RawQuery
    fun getOneNow(query: SupportSQLiteQuery): F?
    fun getOneNow(query: String) = getOneNow(SimpleSQLiteQuery(query))

    fun removeNotKept()

    /**
     * INSERT an [item] into the database,
     * ignoring any conflicts.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun add(item: T): Long
    /**
     * INSERT [items] into the database,
     * ignoring any conflicts.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun addAll(items: List<T>): LongArray

    /**
     * REPLACE an [item] in the database,
     * removing any conflicting rows.
     * Creates the item if it does not exist yet.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun replace(item: T)
    /**
     * REPLACE [items] in the database,
     * removing any conflicting rows.
     * Creates items if it does not exist yet.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun replaceAll(items: List<T>)

    /**
     * Selective UPDATE an [item] in the database.
     * Do nothing if a matching item does not exist.
     */
    fun update(item: T): Long
    /**
     * Selective UPDATE [items] in the database.
     * Do nothing for those items which do not exist.
     */
    fun updateAll(items: List<T>): LongArray

    /**
     * Remove all items from the database,
     * that match the given [profileId].
     */
    fun clear(profileId: Int)

    /**
     * INSERT an [item] into the database,
     * doing a selective [update] on conflicts.
     * @return the newly inserted item's ID or -1L if the item was updated instead
     */
    @Transaction
    fun upsert(item: T): Long {
        val id = add(item)
        if (id == -1L) update(item)
        return id
    }
    /**
     * INSERT [items] into the database,
     * doing a selective [update] on conflicts.
     * @return a [LongArray] of IDs of newly inserted items or -1L if the item existed before
     */
    @Transaction
    fun upsertAll(items: List<T>, removeNotKept: Boolean = false): LongArray {
        val insertResult = addAll(items)
        val updateList = mutableListOf<T>()

        insertResult.forEachIndexed { index, result ->
            if (result == -1L) updateList.add(items[index])
        }

        if (updateList.isNotEmpty()) updateAll(items)
        if (removeNotKept) removeNotKept()
        return insertResult
    }

    /**
     * Make sure that [items] are in the database.
     * When [forceReplace] == false, do a selective update (UPSERT).
     * When [forceReplace] == true, add all items replacing any conflicting ones (REPLACE).
     *
     * @param forceReplace whether to replace all items instead of selectively updating
     * @param removeNotKept whether to remove all items whose [keep] parameter is false
     */
    fun putAll(items: List<T>, forceReplace: Boolean = false, removeNotKept: Boolean = false) {
        if (items.isEmpty())
            return
        if (forceReplace)
            replaceAll(items)
        else
            upsertAll(items, removeNotKept = false)

        if (removeNotKept) removeNotKept()
    }
}
