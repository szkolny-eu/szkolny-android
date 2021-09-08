/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */
package pl.szczodrzynski.edziennik.data.db.dao

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.COLOR_DEFAULT
import pl.szczodrzynski.edziennik.data.db.entity.EventType
import pl.szczodrzynski.edziennik.data.db.entity.EventType.Companion.SOURCE_DEFAULT

@Dao
abstract class EventTypeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun add(eventType: EventType)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun addAll(eventTypeList: List<EventType>)

    @Query("DELETE FROM eventTypes WHERE profileId = :profileId")
    abstract fun clear(profileId: Int)

    @Query("SELECT * FROM eventTypes WHERE profileId = :profileId AND eventType = :typeId")
    abstract fun getByIdNow(profileId: Int, typeId: Long): EventType?

    @Query("SELECT * FROM eventTypes WHERE profileId = :profileId")
    abstract fun getAll(profileId: Int): LiveData<List<EventType>>

    @Query("SELECT * FROM eventTypes WHERE profileId = :profileId")
    abstract fun getAllNow(profileId: Int): List<EventType>

    @get:Query("SELECT * FROM eventTypes")
    abstract val allNow: List<EventType>

    fun addDefaultTypes(context: Context, profileId: Int): List<EventType> {
        var order = 100
        val colorMap = EventType.getTypeColorMap()
        val typeList = EventType.getTypeNameMap().map { (id, name) ->
            EventType(
                profileId = profileId,
                id = id,
                name = context.getString(name),
                color = colorMap[id] ?: COLOR_DEFAULT,
                order = order++,
                source = SOURCE_DEFAULT
            )
        }
        addAll(typeList)
        return typeList
    }
}
