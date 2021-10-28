/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-23.
 */

package pl.szczodrzynski.edziennik.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
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

    @Delete
    fun delete(note: Note)

    @Query("DELETE FROM notes WHERE profileId = :profileId AND noteId = :noteId")
    fun remove(profileId: Int, noteId: Long)

    @Query("SELECT * FROM notes WHERE profileId = :profileId AND noteId = :noteId $ORDER_BY")
    fun get(profileId: Int, noteId: Long): LiveData<Note?>

    @Query("SELECT * FROM notes WHERE profileId = :profileId AND noteId = :noteId $ORDER_BY")
    fun getNow(profileId: Int, noteId: Long): Note?

    @Query("SELECT * FROM notes WHERE profileId = :profileId $ORDER_BY")
    fun getAll(profileId: Int): LiveData<List<Note>>

    @Query("SELECT * FROM notes WHERE profileId = :profileId AND noteOwnerType = :ownerType AND noteOwnerId = :ownerId $ORDER_BY")
    fun getAllFor(profileId: Int, ownerType: Note.OwnerType, ownerId: Long): LiveData<List<Note>>

    @Query("SELECT * FROM notes WHERE profileId = :profileId AND noteOwnerType IS NULL $ORDER_BY")
    fun getAllNoOwner(profileId: Int): LiveData<List<Note>>
}
