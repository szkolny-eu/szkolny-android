/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-23.
 */

package pl.szczodrzynski.edziennik.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import pl.szczodrzynski.edziennik.data.db.entity.Note

@Dao
interface NoteDao {
    companion object {
        private const val ORDER_BY = "ORDER BY addedDate DESC"
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(note: Note)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAll(noteList: List<Note>)

    @Query("SELECT * FROM notes WHERE profileId = :profileId AND noteId = :noteId $ORDER_BY")
    fun get(profileId: Int, noteId: Long): LiveData<Note>

    @Query("SELECT * FROM notes WHERE profileId = :profileId $ORDER_BY")
    fun getAll(profileId: Int): LiveData<List<Note>>

    @Query("SELECT * FROM notes WHERE profileId = :profileId AND noteOwnerType = :ownerType AND noteOwnerId = :ownerId $ORDER_BY")
    fun getAllFor(profileId: Int, ownerType: Note.OwnerType, ownerId: Long): LiveData<List<Note>>
}
